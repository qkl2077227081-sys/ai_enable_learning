package com.kl_v.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kl_v.exam.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /**
     * 查询所有分类和分类题目的数量
     * @return 分类 + count
     */
    List<Category> getCategoryList();

    /**
     * 查询树状分类集合
     * @return 分类+child + count
     */
    List<Category> getCategoryTreeList();

    /**
     * 进行分类新增
     * @param category
     */
    void savaCategory(Category category);
}