package com.kl_v.exam.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kl_v.exam.entity.QuestionChoice;

import java.util.List;

/**
 * 题目选项
 */
public interface QuestionChoiceMapper extends BaseMapper<QuestionChoice> {
    //第二部：根据题目id查询对应的选项集合
    List<QuestionChoice> selectListByQuestionId(Long questionId);
} 