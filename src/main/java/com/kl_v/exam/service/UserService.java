package com.kl_v.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kl_v.exam.common.Result;
import com.kl_v.exam.entity.User;
import com.kl_v.exam.vo.LoginRequestVo;
import com.kl_v.exam.vo.PageResult;

/**
 * 用户Service接口
 * 定义用户相关的业务方法
 */
public interface UserService extends IService<User> {


    /**
     * 登录模块
     *
     * @param loginRequestVo
     * @return
     */
    Result<Object> login(LoginRequestVo loginRequestVo);

    //分页查询
    PageResult<User> pageQuery(Integer pageNum, Integer pageSize, String username);
}