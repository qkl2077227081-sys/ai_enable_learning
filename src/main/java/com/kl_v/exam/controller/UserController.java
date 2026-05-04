package com.kl_v.exam.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import com.kl_v.exam.common.Result;
import com.kl_v.exam.service.UserService;
import com.kl_v.exam.vo.LoginResponseVo;
import com.kl_v.exam.vo.LoginRequestVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.kl_v.exam.entity.User;

import cn.hutool.core.lang.UUID;




/**
 * 用户控制器 - 处理用户认证和权限管理相关的HTTP请求
 * 包括用户登录、权限验证等功能
 */
@RestController  // REST控制器，返回JSON数据
@RequestMapping("/api/user")  // 用户API路径前缀
@CrossOrigin(origins = "*")  // 允许跨域访问
@Tag(name = "用户管理", description = "用户相关操作，包括登录认证、权限验证等功能")  // Swagger API分组
public class UserController {


    @Autowired
    private UserService userService;
    /**
     * 用户登录
     * @param loginRequestVo 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "明文比对练习版：校验用户名和密码是否一致")
    public Result<LoginResponseVo> login(@RequestBody LoginRequestVo loginRequestVo) throws InterruptedException {
        // 1. 参数基础校验
        if (loginRequestVo == null || StrUtil.isBlank(loginRequestVo.getUsername())) {
            return Result.error("用户名不能为空");
        }

        // 2. 数据库查询用户
        // 假设你使用的是 MyBatis-Plus，通过 username 查询唯一用户
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginRequestVo.getUsername()));

        // 3. 明文比对 (练习阶段直接使用 == 或 .equals)
        if (user == null || !user.getPassword().equals(loginRequestVo.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        // 4. 状态校验 (对应你前端页面显示的 ACTIVE/DISABLED)
        if ("DISABLED".equals(user.getStatus())) {
            return Result.error("该账号已被禁用，请联系管理员");
        }

        // 5. 组装返回数据
        LoginResponseVo responseVo = new LoginResponseVo();
        // 修复第 5 步：使用 Hutool 的 fastUUID 或原生 UUID
        responseVo.setToken("mock-token-" + cn.hutool.core.lang.UUID.fastUUID().toString());
        responseVo.setUsername(user.getUsername());
        responseVo.setRealName(user.getRealName());
        responseVo.setRole(user.getRole());

        return Result.success(responseVo);
    }


} 