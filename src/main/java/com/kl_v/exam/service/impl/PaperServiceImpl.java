package com.kl_v.exam.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kl_v.exam.entity.Paper;
import com.kl_v.exam.entity.PaperQuestion;
import com.kl_v.exam.entity.Question;
import com.kl_v.exam.mapper.PaperMapper;
import com.kl_v.exam.mapper.QuestionMapper;
import com.kl_v.exam.service.PaperQuestionService;
import com.kl_v.exam.service.PaperService;
import com.kl_v.exam.service.QuestionService;
import com.kl_v.exam.vo.AiPaperVo;
import com.kl_v.exam.vo.PaperVo;
import com.kl_v.exam.vo.RuleVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 试卷服务实现类
 */
@Slf4j
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private PaperQuestionService paperQuestionService;


    /**
     * 根据试卷id的查询试卷详情
     * 试卷对象
     * 题目集合
     * ps：题目的选项sort正序
     * ps：所有题目根据类型排序
     *
     * @param id 试卷id
     * @return
     */
    @Override
    public Paper customPaperDetailById(Long id) {
//        单表java代码进行paper查询
        Paper paper = getById(id);
//        校验paper == null -> 抛异常
        if (paper == null) {
            throw new RuntimeException("指定id=%s的试卷已经被删除无法查看详情".formatted(id));
        }
//        根据paperid查询题目集合（中间，题目，答案，选项）
        List<Question> questionList = questionMapper.customQuertQusetionListByPaperId(id);
//        校验题目集合 == null -> 赋空集合！ log->做好记录
        if (ObjectUtils.isEmpty(questionList)) {
            paper.setQuestions(new ArrayList<Question>());
            log.warn("试卷中没有题目！可以进行试卷编辑！但是不可用于考试！！对应的试卷id：{}", id);
            return paper;
        }

        log.debug("题目信息排序前：{}",questionList);
//        对题目进行排序（选择 -> 判断 -> 简答）
        questionList.sort((o1, o2) -> Integer.compare(typeToInt(o1.getType()), typeToInt(o2.getType())));
        log.debug("题目信息排序后：{}",questionList);
//        注意：type排序，是字符类型 -》 字符 -》 对应 -》 固定的数字 1 2 3
//        进行paper题目集合赋值
        paper.setQuestions(questionList);
        return paper;
    }

    /**
     * 手动组卷
     *
     * @param paperVo
     * @return
     */
    @Override
    public Paper customCreatePaper(PaperVo paperVo) {
        //1. 完善试卷内信息 名字 描述 时间  -> 状态 ，总题目数 ， 总分数
        Paper paper = new Paper();
        //名字 描述 时间
        BeanUtils.copyProperties(paperVo,paper);
        //进行名字的校验
        LambdaQueryWrapper<Paper> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Paper::getName,paper.getName());
        boolean exists = baseMapper.exists(queryWrapper);
        if (exists) {
            throw new RuntimeException("在当前页面里已经存在%s，不可重复".formatted(paper.getName()));
        }
        //态，总题目数，总分数
        paper.setStatus("DRAFT");

        if(ObjectUtils.isEmpty(paperVo.getQuestions())){
            //本次没选题目
            paper.setTotalScore(BigDecimal.ZERO);
            paper.setQuestionCount(0);
            save(paper);
            log.warn("本次{}组卷没有选择题目！注意没有题目的试卷无法进行考试！！",paper);
            return paper;
        }
        /*
        状态默认值：DRAFT
        总题目数：question
        总分数：question分数的和
         */
        paper.setQuestionCount(paperVo.getQuestions().size());
        paper.setTotalScore(paperVo.getQuestions().values().stream().reduce(BigDecimal.ZERO,BigDecimal::add));
        //2. 完成试卷的插入 -》 主键回显 paperId
        save(paper);
//        3. questions -> 中间表集合 （题目id,试卷id,分数）
        List<PaperQuestion> paperQuestionList = paperVo.getQuestions().entrySet().stream().map(entry -> {
            PaperQuestion paperQuestion =
                    new PaperQuestion(paper.getId().intValue(), Long.valueOf(entry.getKey()), entry.getValue());
            return paperQuestion;
        }).collect(Collectors.toList());
//        4. 中间表集合插入 【批量插入】 -》 中间表的service对象
        paperQuestionService.saveBatch(paperQuestionList);
//        5. 返回对应paper对象
        return paper;
    }

    /**
     * ai智能组卷
     *
     * @param aiPaperVo
     * @return
     */
    @Override
    public Paper customAiCreatePaper(AiPaperVo aiPaperVo) {
//        试卷基本信息 + 草稿状态 进行保存 （回显试卷的id）
        Paper paper = new Paper();
        BeanUtils.copyProperties(aiPaperVo,paper);
        //进行名字的校验
        LambdaQueryWrapper<Paper> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Paper::getName,paper.getName());
        boolean exists = baseMapper.exists(lambdaQueryWrapper);
        if (exists) {
            throw new RuntimeException("在当前页面里已经存在%s，不可重复".formatted(paper.getName()));
        }
        paper.setStatus("DRAFT");
        save(paper);
        //2.组卷规则下的试题选择和中间表保存
        int questionCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;

        for (RuleVo rule : aiPaperVo.getRules()) {
            //需要校验规则下的题目数量 = 0 跳过
            if(rule.getCount() == 0){
                log.warn("在：{}类型下，不需要出题",rule.getType().name());
                continue;
            }
            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Question::getType,rule.getType().name());
            queryWrapper.in(!ObjectUtils.isEmpty(rule.getCategoryIds()),Question::getCategoryId,rule.getCategoryIds());
            List<Question> AllQuestionList = questionMapper.selectList(queryWrapper);
            //步骤三校验查询的题目集合 集合为空
            if (ObjectUtils.isEmpty(AllQuestionList)) {
                log.warn("在：{}类型下，我们制定的分类：{}没有查询到题目信息",rule.getType().name(),rule.getCategoryIds());
                continue;
            }
            //步骤四判断是否有规则下count数量，没有全都要了
            int realNumbers = Math.min(rule.getCount(), AllQuestionList.size());
            //步骤五：本次规则下添加的数量和分数累加
            questionCount+=realNumbers;
            totalScore = totalScore.add(BigDecimal.valueOf((long) realNumbers * rule.getScore()));
            //步骤六：先打乱顺序，在截取需要的题目数量
            Collections.shuffle(AllQuestionList);
            List<Question> realQuestionList = AllQuestionList.subList(0, realNumbers);
            //步骤七转成中间表并进行保存
            List<PaperQuestion> paperQuestionList = realQuestionList.stream().map(question -> {
                PaperQuestion paperQuestion = new PaperQuestion(paper.getId().intValue(), question.getId(), BigDecimal.valueOf(rule.getScore()));
                return paperQuestion;
            }).collect(Collectors.toList());
            paperQuestionService.saveBatch(paperQuestionList);
        }
        //修改试卷信息（总题目数，总分数）
        paper.setQuestionCount(questionCount);
        paper.setTotalScore(totalScore);
        updateById(paper);

        return paper;
    }

    /**
     * 更新试卷信息
     *
     * @param id
     * @param paperVo
     * @return
     */
    @Override
    public Paper customUpdatePaper(Integer id, PaperVo paperVo) {
        //校验（不能发布状态，不同id name相同）
        Paper paper =getById(id);
        if("PUBLISHED".equals(paper.getStatus())){
            throw new RuntimeException("发布状态下的试卷不允许修改");
        }
        //检验id，name
        LambdaQueryWrapper<Paper> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Paper::getId,id);
        queryWrapper.eq(Paper::getName,paperVo.getName());
        long count = count(queryWrapper);
        if (count > 0) {
            throw new RuntimeException("%s试卷名字已经存在，请重新修改".formatted(paperVo.getName()));
        }
        //试卷的主题信息
        BeanUtils.copyProperties(paperVo,paper);
        //分和题目数量
  /*
        状态默认值：DRAFT
        总题目数：question
        总分数：question分数的和
         */
        paper.setQuestionCount(paperVo.getQuestions().size());
        paper.setTotalScore(paperVo.getQuestions().values().stream().reduce(BigDecimal.ZERO,BigDecimal::add));
        //2. 完成试卷的插入 -》 主键回显 paperId
        updateById(paper);
//        3.中间表先删除后保存
        paperQuestionService.remove(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId,paper.getId()));
        List<PaperQuestion> paperQuestionList = paperVo.getQuestions().entrySet().stream().map(entry -> {
            PaperQuestion paperQuestion =
                    new PaperQuestion(paper.getId().intValue(), Long.valueOf(entry.getKey()), entry.getValue());
            return paperQuestion;
        }).collect(Collectors.toList());
//        4. 中间表集合插入 【批量插入】 -》 中间表的service对象
        paperQuestionService.saveBatch(paperQuestionList);
//        5. 返回对应paper对象
        return paper;

    }

    /**
     * 更新试卷状态
     *
     * @param id
     * @param status
     */
    @Override
    public void customUpdatePaperStatus(Integer id, String status) {
        //判断目标状态-》发布 -》查询试卷的题目数量
        if("PUBLISHED".equals(status)){
            LambdaQueryWrapper<PaperQuestion> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PaperQuestion::getPaperId,id);
            long count = paperQuestionService.count(queryWrapper);
            if (count == 0) {
                throw new RuntimeException("状态修改失败！！目标发布状态下的试卷必须有题目！");
            }
        }
        //正常修改状态即可
        LambdaUpdateWrapper<Paper> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Paper::getStatus,status);
        updateWrapper.eq(Paper::getId,id);
        update(updateWrapper);
    }

    //给予类型赋值
    private int typeToInt(String type) {
        switch (type) {
            case "CHOICE":return 1;
            case "JUDGE":return 2;
            case "TEXT":return 3;
            default:return 4;
        }
    }
}