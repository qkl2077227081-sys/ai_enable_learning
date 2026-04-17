package com.kl_v.exam.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: MybatisPlusConfiguration
 * Package: com.kl_v.exam.config
 * Description:
 *
 * @Author V
 * @Create 2026/4/17 下午3:17
 * @Version 1.0
 */
@Configuration
@MapperScan("com.kl_v.exam.mapper")
public class MybatisPlusConfiguration {

}
