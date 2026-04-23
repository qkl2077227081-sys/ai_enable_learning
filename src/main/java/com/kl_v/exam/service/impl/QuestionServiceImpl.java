package com.kl_v.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kl_v.exam.common.CacheConstants;
import com.kl_v.exam.entity.PaperQuestion;
import com.kl_v.exam.entity.Question;

import com.kl_v.exam.entity.QuestionAnswer;
import com.kl_v.exam.entity.QuestionChoice;
import com.kl_v.exam.mapper.PaperQuestionMapper;
import com.kl_v.exam.mapper.QuestionAnswerMapper;
import com.kl_v.exam.mapper.QuestionChoiceMapper;
import com.kl_v.exam.service.QuestionService;
import com.kl_v.exam.mapper.QuestionMapper;
import com.kl_v.exam.utils.RedisUtils;
import com.kl_v.exam.vo.QuestionPageVo;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.management.RuntimeMBeanException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题目Service实现类
 * 实现题目相关的业务逻辑
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;
    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Override
    public void customPageService(Page<Question> pageBean, QuestionPageVo questionPageVo) {
        questionMapper.customPage(pageBean,questionPageVo);
    }

    @Override
    public void customPageJavaService(Page<Question> pageBean, QuestionPageVo questionPageVo) {
        //1.分页查询列表
        LambdaQueryWrapper<Question> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(!ObjectUtils.isEmpty(questionPageVo.getType()), Question::getType,questionPageVo.getType());
        lambdaQueryWrapper.eq(!ObjectUtils.isEmpty(questionPageVo.getDifficulty()),Question::getDifficulty,questionPageVo.getDifficulty());
        lambdaQueryWrapper.eq(!ObjectUtils.isEmpty(questionPageVo.getCategoryId()),Question::getCategoryId,questionPageVo.getCategoryId());
        lambdaQueryWrapper.like(!ObjectUtils.isEmpty(questionPageVo.getKeyword()),Question::getTitle,questionPageVo.getKeyword());
        //根据时间进行倒叙排序
        lambdaQueryWrapper.orderByDesc(Question::getCreateTime);
        page(pageBean,lambdaQueryWrapper);

        //2.提取一个方法，给题目进行选项和答案装填
        fillQuestionChoiceAndAnswer(pageBean.getRecords());
    }

    @Override
    public Question customDetailQuestion(Long id) {
        //1.查询题目详情
        Question question = questionMapper.customGetById(id);
        if (question == null){
            throw  new RuntimeException("题目查询详情失败！原因可能提前被删除！题目id为：" + id);
        }
        //2.进行热点题目缓存
        new Thread(() -> {
            incrementQuestion(question.getId());
        }).start();
        return question;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void customSaveQuestion(Question question) {
        //1.一定插入题目信息 （回显题目id）
        //同一个类下title不能相同
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getType,question.getType());
        queryWrapper.eq(Question::getTitle,question.getTitle());
        //自己的业务获取自己的方法
        boolean exists = baseMapper.exists(queryWrapper);
        if (exists) {
            //同一类型title相同
            throw new RuntimeException("在%s下，存在%s 名称的题目已经存在！无法保存".formatted(question.getTitle(),question.getType()));
        }
        boolean saved = save(question);
        if (!saved) {
            //同一类型title相同
            throw new RuntimeException("在%s下，存在%s 名称的题目！无法保存".formatted(question.getTitle(),question.getType()));
        }
        //2.获取答案对象，并先配置题目id
        QuestionAnswer answer = question.getAnswer();
        answer.setQuestionId(question.getId());
        //3.判断是不是选择题
        if("CHOICE".equals(question.getType())){
        //是 -》 循环 -》 选项 + 题目id -> 保存 -》 判断是不是正确 进行 A,B,C
        List<QuestionChoice> choices = question.getChoices();
        StringBuilder sb = new StringBuilder();
            for (int i = 0; i < choices.size(); i++) {
                QuestionChoice choice = choices.get(i);
                choice.setQuestionId(question.getId());
                questionChoiceMapper.insert(choice);
                if (choice.getIsCorrect()) {
                    //true 本次是正确答案
                    if (sb.length()>0) {
                        sb.append(",");
                    }
                    sb.append((char)('A'+i));
                }
            }
            //进行答案赋值
            answer.setAnswer(sb.toString());
        //4.保存答案对象
            questionAnswerMapper.insert(answer);
        //5.保证方法的一致性！ 需要添加事务
    }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void customUpdateQuestion(Question question) {
//        1. 题目的校验 （不同id不运行title重复）
        LambdaQueryWrapper<Question> QueryWrapper = new LambdaQueryWrapper<>();
        QueryWrapper.ne(Question::getId,question.getId());
        QueryWrapper.eq(Question::getTitle,question.getTitle());
        boolean exists = baseMapper.exists(QueryWrapper);
        if (exists) {
            throw new RuntimeException("修改：%s的新标题：%s和其他新标题重复了".formatted(question.getId(),question.getTitle()));
        }
//        2. 修改题目
        boolean updated = updateById(question);
        if (!updated) {
            throw new RuntimeException("修改：%s题目失败".formatted(question.getId()));
        }
//        3. 获取答案对象
        QuestionAnswer answer = question.getAnswer();
//        4. 判断是选择题
        if ("CHOICE".equals(question.getType())) {
            List<QuestionChoice> choiceList = question.getChoices();
            //删除题目对应的所有选项（原）[根据题目id删除]
            LambdaQueryWrapper<QuestionChoice> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(QuestionChoice::getQuestionId,question.getId());
            questionChoiceMapper.delete(lambdaQueryWrapper);
            //循环新增选项（选项上id == null）
            //拼接正确的档案 a,b
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < choiceList.size(); i++) {
                QuestionChoice choice = choiceList.get(i);
                choice.setId(null);
                choice.setCreateTime(null);
                choice.setUpdateTime(null);
                //新增选项的需要
                choice.setQuestionId(question.getId());
                questionChoiceMapper.insert(choice);
                if (choice.getIsCorrect()) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append((char) ('A' + i));
                }
            }
            //答案对象赋值选择题答案
            answer.setAnswer(sb.toString());

        }
//        5. 进行答案的修改
        questionAnswerMapper.updateById(answer);
//        6. 保证一致性，添加事务

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void customRemoveQuestionById(Long id) {
//        1. 判断试卷题目表，存在删除失败！
        LambdaQueryWrapper<PaperQuestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaperQuestion::getQuestionId,id);
        Long count = paperQuestionMapper.selectCount(queryWrapper);
        if (count >0){
            throw new RuntimeException("改题目：%s被试卷表中引用%s次，删除失败！".formatted(id,count));
        }
//        2. 删除主表 题目表
        boolean removed = removeById(id);
        if(!removed){
            throw new RuntimeException("改题目：%信息删除失败！".formatted(id));
        }
//        3. 删除子表 答案和选项表
        questionAnswerMapper.delete(new LambdaQueryWrapper<QuestionAnswer>().eq(QuestionAnswer::getQuestionId,id));
        questionChoiceMapper.delete(new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId,id));
    }

    @Override
    public List<Question> customFindPopularQuestions(Integer size) {
        //1. 定义热门题目集合（总集合）
        List<Question> popularQuestions = new ArrayList<>();

        //2. 去zset中获取热门题目，并且添加到总集合中
        // 获取题目排行，需要获取id和分数！ 分数用于后续的排序处理！
        Set<ZSetOperations.TypedTuple<Object>> tupleSet = redisUtils.zReverseRangeWithScores(CacheConstants.POPULAR_QUESTIONS_KEY, 0, size - 1);
        //定义接收id的集合
        List<Long> idsSet = new ArrayList<>();
        if (tupleSet != null && tupleSet.size() > 0) {
            //根据排行榜的积分，倒序进行Id查询！
            List<Long> idsList = tupleSet.stream().sorted((o1, o2) -> Integer.compare(o2.getScore().intValue(), o1.getScore().intValue()))
                    .map(o -> Long.valueOf(o.getValue().toString())).collect(Collectors.toList());
            //复制，用于后面补充！！
            idsSet.addAll(idsList);
            log.debug("从redis获取热门题目的id集合，且保证顺序：{}",idsList);

            for (Long id : idsList) {
                Question question = getById(id);
                if (question != null){
                    //防止redis有缓存，但是数据库中没有！ 后续优化，删除题目，应该删除热题榜单中对应的value
                    popularQuestions.add(question);
                }
            }
            log.debug("去redis查询的热门题目，题目数：{},题目内容为：{}",popularQuestions.size(),popularQuestions);
        }

        //3. 检查是否已经满足size
        int diff = size - popularQuestions.size();
        if(diff > 0){
            //4. 不满足，题目表中 非热门题目 时间倒序 limit 差数量
            LambdaQueryWrapper<Question> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.notIn(Question::getId,idsSet);
            lambdaQueryWrapper.orderByDesc(Question::getCreateTime);
            //limit diff;
            lambdaQueryWrapper.last("limit " + diff);
            List<Question> questionDiffList = list(lambdaQueryWrapper);
            log.debug("去question表中补充热门题目，题目数：{},题目内容为：{}",questionDiffList.size(),questionDiffList);
            if (questionDiffList != null && questionDiffList.size() > 0) {
                // 5. 补充也添加到总集合中
                popularQuestions.addAll(questionDiffList);
            }
        }
        //6. 总集合一起进行答案和选项填充
        fillQuestionChoiceAndAnswer(popularQuestions);
        //7. 返回即可
        return popularQuestions;
    }

    //定义进行题目访问次数增长的方法
    //异步方法
    private void incrementQuestion(Long questionId){
        Double score = redisUtils.zIncrementScore(CacheConstants.POPULAR_QUESTIONS_KEY,questionId,1);
        log.info("完成{}题目分数累计，累计后分数为：{}",questionId,score);
    }

    private void fillQuestionChoiceAndAnswer(List<Question> questionList) {
        //1. 非空判断
        if (questionList == null || questionList.size() == 0) {
            log.debug("没有查询对应的问题集合数据！！");
            return;
        }
        //2. 查询所有答案和选项
        //优化查询本次题目的答案和选项
        //查询本地题目集合对应的id集合！！
        List<Long> ids = questionList.stream().map(Question::getId).collect(Collectors.toList());
        //查询本次题目的选项集合
        List<QuestionChoice> questionChoiceList =
                questionChoiceMapper.selectList(new LambdaQueryWrapper<QuestionChoice>().in(QuestionChoice::getQuestionId, ids));
        //查询本次题目的答案
        List<QuestionAnswer> questionAnswers =
                questionAnswerMapper.selectList(new LambdaQueryWrapper<QuestionAnswer>().in(QuestionAnswer::getQuestionId, ids));
        //3. 答案和选项进行map转化
        Map<Long, List<QuestionChoice>> questionChoiceMap =
                questionChoiceList.stream().collect(Collectors.groupingBy(QuestionChoice::getQuestionId));
        Map<Long, QuestionAnswer> answerMap =
                questionAnswers.stream().collect(Collectors.toMap(QuestionAnswer::getQuestionId, a -> a));
        //4. 循环问题集合，进行选项和答案配置
        questionList.forEach(question -> {
            //每个题目一定答案
            question.setAnswer(answerMap.get(question.getId()));
            //选择题才有选项
            if ("CHOICE".equals(question.getType())) {
                List<QuestionChoice> questionChoices = questionChoiceMap.get(question.getId());
                questionChoices.sort(Comparator.comparingInt(QuestionChoice::getSort));
                question.setChoices(questionChoices);
            }
        });
    }}