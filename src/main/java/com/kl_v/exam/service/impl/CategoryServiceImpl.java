package com.kl_v.exam.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kl_v.exam.entity.Category;
import com.kl_v.exam.entity.Question;
import com.kl_v.exam.mapper.CategoryMapper;
import com.kl_v.exam.mapper.QuestionMapper;
import com.kl_v.exam.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper,Category> implements CategoryService {

    @Autowired
    private QuestionMapper questionMapper;



    @Override
    public List<Category> getCategoryList() {
        //步骤1：查询所有分类信息
        List<Category> categoryList = list();
        //步骤二：完成分类分析的题目数量填充[tree也要使用]
        fillCategoryCount(categoryList);
        //步骤三：返回完整结果
        return categoryList;
    }

    @Override
    public List<Category> getCategoryTreeList() {
        //步骤1：查询所有分类信息
        List<Category> categoryList = list();
        //步骤二：完成分类分析的题目数量填充[tree也要使用]
        fillCategoryCount(categoryList);
        //对集合进行分组
        Map<Long, List<Category>> parentIdMap = categoryList.stream().collect(Collectors.groupingBy(Category::getParentId));
        //筛选出parent_id = 0
        List<Category> categoriyListResult = categoryList.stream().filter(category -> category.getParentId() == 0).collect(Collectors.toList());
        //对筛选的集合进行赋值
        categoriyListResult.forEach(category -> {
            //赋值子分类
            List<Category> childCategoryList = parentIdMap.getOrDefault(category.getId(), new ArrayList<>());
            //排序
//            childCategoryList.sort((o1, o2) -> o1.getSort()-o2.getSort());
            childCategoryList.sort(Comparator.comparingInt(Category::getSort));
            category.setChildren(childCategoryList);
            //赋值count = 当前count + 子分类count
            long childCount = childCategoryList.stream().mapToLong(Category::getCount).sum();
            category.setCount(category.getCount()+childCount);
        });
        return categoriyListResult;
    }

    @Override
    public void savaCategory(Category category) {
        //判断同一个父类分类下不允许重名
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Category::getParentId,category.getParentId());
        lambdaQueryWrapper.eq(Category::getName,category.getName());
        long count = count(lambdaQueryWrapper);

        if(count>0){
            Category parent = getById(category.getParentId());
            //不能添加，同一个父类下名称重复了
            throw new RuntimeException("在%s父分类下，已经存在名为：%s的子分类，本次添加失败".formatted(parent.getName(),category.getName()));
        }
        //2.保存
        save(category);
    }

    @Override
    public void updateCategory(Category category) {
        //判断同一个父类分类下不允许重名
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper();
        lambdaQueryWrapper.eq(Category::getParentId,category.getParentId());
        lambdaQueryWrapper.ne(Category::getId,category.getId());
        lambdaQueryWrapper.eq(Category::getName,category.getName());
        CategoryMapper categoryMapper = getBaseMapper();
        boolean exists = categoryMapper.exists(lambdaQueryWrapper);
        if (exists) {
            Category parent = getById(category.getParentId());
            //不能添加，同一个父类下名称重复了
            throw new RuntimeException("在%s父类下，已经存在名为%s子分类，本次更新失败".formatted(parent.getName(),category.getName()));
        }
        //更新
        updateById(category);
    }

    @Override
    public void removeCategoryById(Long id) {
        //1.检查是否一级标题
        Category category = getById(id);
        if(category.getParentId() ==0){
            throw new RuntimeException("不能删除一级标题");
        }
        //2.检查是否存在关联题目
        LambdaQueryWrapper<Question> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Question::getCategoryId,id);
        Long count = questionMapper.selectCount(lambdaQueryWrapper);
        if (count>0) {
            throw new RuntimeException("当前的：%s分类，关联了%s道题目，无法删除！！".formatted(category.getName(),count));
        }
        //3.以上都不满足删除即可
        removeById(id);
    }

    /**
     * 给予分类信息进行count填充
     * 1.判断分类集合是否为empty【对一个集合，对一个数据进行大量逻辑代码之前，尽量先判断，否则出现无用功】
     * 2.查询所有分类和对应的题目数量【mapper方法】
     * 3.进行分类数据和count转化，map<categoryId,count>
     * 4.给集合每一个分类查询count并赋值
     * @param categoryList
     */
    private void fillCategoryCount(List<Category> categoryList){
        if(categoryList == null ||categoryList.isEmpty()){
            throw new RuntimeException("查询分类集合为空");
        }
        //查询所有分类和分类对应的题目数量【mapper方法】
        //查询题目中的分类数量
        List<Map<Long, Long>> mapList = questionMapper.selectCategoryCount();
        Map<Long,Long> resultCount = mapList.stream().collect(Collectors.toMap(m -> m.get("category_id"), m -> m.get("ct")));
        //进行分类结果填充
        categoryList.forEach(category -> {
            category.setCount(resultCount.getOrDefault(category.getId(),0L));
        });


    }
}