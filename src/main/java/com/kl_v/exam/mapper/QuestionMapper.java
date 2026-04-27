package com.kl_v.exam.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kl_v.exam.entity.Question;
import com.kl_v.exam.vo.QuestionPageVo;
import org.apache.ibatis.annotations.Param;
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

    /**
     * 分页查询题目信息
     * @param page 分页对象
//     * @param questionPageVo 自己实体类
     * @return
     */
    IPage<Question> customPage(IPage page, @Param("queryVo") QuestionPageVo queryVo);


    Question customGetById(Long questionId);

    /**
     * 根据试卷id查询题目集合
     * @param paperId
     * @return
     */
    List<Question> customQuertQusetionListByPaperId(Long paperId);
}