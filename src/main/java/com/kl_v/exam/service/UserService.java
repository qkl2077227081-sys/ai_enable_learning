package com.kl_v.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kl_v.exam.common.Result;
import com.kl_v.exam.entity.User;
import com.kl_v.exam.vo.LoginRequestVo;

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
}