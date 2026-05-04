package com.kl_v.exam.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kl_v.exam.entity.VideoCategory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 视频分类Mapper接口
 * 提供视频分类相关的数据访问操作
 */
@Mapper
public interface VideoCategoryMapper extends BaseMapper<VideoCategory> {
    
    /**
     * 获取每个分类的视频数量统计
     * @return 包含分类ID和视频数量的结果列表
     */
    List<Map<String, Object>> getCategoryVideoCount();
    
    /**
     * 获取所有启用的顶级分类
     * @return 顶级分类列表
     */
    List<VideoCategory> getTopCategories();
    
    /**
     * 根据父级分类ID获取子分类
     * @param parentId 父级分类ID
     * @return 子分类列表
     */
    List<VideoCategory> getChildCategories(Long parentId);
} 