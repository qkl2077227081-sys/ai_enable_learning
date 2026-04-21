package com.kl_v.exam.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kl_v.exam.entity.QuestionChoice;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目选项
 */
public interface QuestionChoiceMapper extends BaseMapper<QuestionChoice> {
    //第二部：根据题目id查询对应的选项集合
    @Select("select * from question_choices where is_deleted = 0 and question_id = #{questionId} order by sort asc")
    List<QuestionChoice> selectListByQuestionId(Long questionId);
} 