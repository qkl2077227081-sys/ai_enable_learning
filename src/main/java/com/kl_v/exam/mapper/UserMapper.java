package com.kl_v.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kl_v.exam.entity.User;

/**
 * 用户Mapper接口
 * 继承MyBatis Plus的BaseMapper，提供基础的CRUD操作
 */
public interface UserMapper extends BaseMapper<User> {
    // 可以在这里添加自定义的查询方法
} 