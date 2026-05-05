package com.kl_v.exam.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import com.kl_v.exam.common.Result;
import com.kl_v.exam.service.UserService;
import com.kl_v.exam.vo.LoginResponseVo;
import com.kl_v.exam.vo.LoginRequestVo;
import com.kl_v.exam.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.kl_v.exam.entity.User;

import java.util.List;


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
    @Operation(summary = "用户登录", description = "明文比对：校验用户名和密码是否一致")
    public Result<LoginResponseVo> login(@RequestBody LoginRequestVo loginRequestVo) throws InterruptedException {
        // 1. 参数基础校验
        if (loginRequestVo == null || StrUtil.isBlank(loginRequestVo.getUsername())) {
            return Result.error("用户名不能为空");
        }

        // 2. 数据库查询用户
        // 假设你使用的是 MyBatis-Plus，通过 username 查询唯一用户
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginRequestVo.getUsername()));


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

    @GetMapping("/list")
    @Operation(summary = "分页查询用户列表")
    @ResponseBody
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String username) {

        // 调用 Service 获取封装好的 PageResult
        PageResult<User> pageData = userService.pageQuery(pageNum, pageSize, username);


        return Result.success(pageData);
    }

    @PostMapping("/add")
    @Operation(summary = "新增用户")
    public Result<String> add(@RequestBody User user) {
        // 状态默认为 ACTIVE
        if (StrUtil.isBlank(user.getStatus())) {
            user.setStatus("ACTIVE");
        }
        userService.save(user);
        return Result.success("添加成功");
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户")
    public Result<String> update(@RequestBody User user) {
        // 逻辑更新，MyBatis Plus 会自动处理 updateTime (如果配置了拦截器)
        userService.updateById(user);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "单个删除（逻辑删除）")
    public Result<String> delete(@PathVariable Long id) {
        // 因为标记了 @TableLogic，这里实际是更新 is_deleted 字段
        userService.removeById(id);
        return Result.success("删除成功");
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        userService.removeByIds(ids);
        return Result.success("批量删除成功");
    }




} 