package com.kl_v.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kl_v.exam.common.CacheConstants;
import com.kl_v.exam.entity.Question;

import com.kl_v.exam.entity.QuestionAnswer;
import com.kl_v.exam.entity.QuestionChoice;
import com.kl_v.exam.mapper.QuestionAnswerMapper;
import com.kl_v.exam.mapper.QuestionChoiceMapper;
import com.kl_v.exam.service.QuestionService;
import com.kl_v.exam.mapper.QuestionMapper;
import com.kl_v.exam.utils.RedisUtils;
import com.kl_v.exam.vo.QuestionPageVo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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