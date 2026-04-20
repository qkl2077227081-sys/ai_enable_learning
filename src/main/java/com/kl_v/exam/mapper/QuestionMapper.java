package com.kl_v.exam.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kl_v.exam.entity.Question;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 题目Mapper接口
 * 继承MyBatis Plus的BaseMapper，提供基础的CRUD操作
 */
public interface QuestionMapper extends BaseMapper<Question> {
    /**
     * 查询题目中分类的数量
     * @return Map<分类id，题目数量>
     */
    @Select("select category_id,count(*) ct from questions where is_deleted = 0 GROUP BY category_id")
    List<Map<Long,Long>> selectCategoryCount();

} 