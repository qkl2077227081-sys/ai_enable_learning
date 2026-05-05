package com.kl_v.exam.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: Kinfe4jConfiguration
 * Package: com.kl_v.exam.config
 * Description:
 *
 * @Author V
 * @Create 2026/4/17 下午3:23
 * @Version 1.0
 */
@Configuration
public class Kinfe4jConfiguration {
    /**
     * 配置OpenAPI文档信息
     */
    @Bean
    public OpenAPI customOpenAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("智能考试系统API文档")
                        .description("智能考试系统后端接口文档，提供完整的RESTful API服务\n\n"+
                                "主要功能模块：\n"+
                                "题目管理 \n"+
                                "试卷管理 \n"+
                                "轮播图管理 \n"+
                                "考试记录 \n"+
                                "公告管理 \n"
                        ).version("v1.0.0")
                );
    }

    //用户管理两套结构
    @Bean
    public GroupedOpenApi userAPI(){
        return GroupedOpenApi.builder()
                .group("用户信息管理")
                .pathsToMatch(
                        "/api/user/**"
                ).build();
    }
    //试题信息管理
    @Bean
    public GroupedOpenApi questionAPI(){
        return GroupedOpenApi.builder()
                .group("试题信息管理")
                .pathsToMatch(
                        "/api/categories/**",
                        "/api/questions/**"
                ).build();
    }
    //考试信息管理（试卷，考试和考试记录）
    @Bean
    public GroupedOpenApi papersAPI() {
        return GroupedOpenApi.builder()
                .group("考试信息管理")
                .pathsToMatch(
                        "/api/papers/**",
                        "/api/exams/**",
                        "/api/exam-records/**"
                ).build();
    }
    //视频信息管理（管理端视频，客户端视频，视频分类）
    @Bean
    public GroupedOpenApi videosAPI() {
        return GroupedOpenApi.builder()
                .group("视频信息管理")
                .pathsToMatch(
                        "/api/admin/videos/**",
                        "/api/videos/**",
                        "/api/video-categories/**"
                ).build();
    }
    //系统信息管理（公告和轮播图）
    @Bean
    public GroupedOpenApi systemAPI() {
        return GroupedOpenApi.builder()
                .group("系统信息管理")
                .pathsToMatch(
                        "/api/banners/**",
                        "/api/notices/**"
                ).build();
    }

    //其他信息管理（首页数据，查看文件，debug）
    @Bean
    public GroupedOpenApi otherAPI() {
        return GroupedOpenApi.builder()
                .group("其他信息管理")
                .pathsToMatch(
                        "/api/stats/**",
                        "/files/**",
                        "/api/debug/**"
                ).build();
    }
}
