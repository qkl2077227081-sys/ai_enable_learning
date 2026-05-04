package com.kl_v.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kl_v.exam.common.Result;
import com.kl_v.exam.entity.User;
import com.kl_v.exam.service.UserService;
import com.kl_v.exam.mapper.UserMapper;
import com.kl_v.exam.vo.LoginRequestVo;
import com.kl_v.exam.vo.LoginResponseVo;
import org.springframework.stereotype.Service;

/**
 * 用户Service实现类
 * 实现用户相关的业务逻辑
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public Result<Object> login(LoginRequestVo loginRequestVo) {
        // 1. 根据用户名查询用户
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginRequestVo.getUsername()));

        // 2. 校验用户是否存在
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 3. 练习阶段：明文比对密码
        if (!user.getPassword().equals(loginRequestVo.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        // 4. 校验账号状态 (ACTIVE/DISABLED)
        if ("DISABLED".equals(user.getStatus())) {
            return Result.error("账号已被禁用");
        }

        // 5. 封装响应数据
        LoginResponseVo responseVo = new LoginResponseVo();
        responseVo.setUserId(user.getId());
        responseVo.setUsername(user.getUsername());
        responseVo.setRealName(user.getRealName());
        responseVo.setRole(user.getRole());

        // 生成练习用的 Mock Token
        // 注意：这里使用了 Hutool 的 UUID 工具，如果没导包，请换成 java.util.UUID
        responseVo.setToken("mock-token-" + cn.hutool.core.lang.UUID.fastUUID().toString());

        return Result.success(responseVo);
    }
}