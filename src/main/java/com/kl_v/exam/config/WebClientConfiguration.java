package com.kl_v.exam.config;

import com.kl_v.exam.config.properties.DeepSeekProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ClassName: WebClientConfiguration
 * Package: com.kl_v.exam.config
 * Description:
 *
 * @Author V
 * @Create 2026/4/24 下午7:22
 * @Version 1.0
 */
@EnableConfigurationProperties(DeepSeekProperties.class)
@Configuration
public class WebClientConfiguration {
    @Autowired
    private DeepSeekProperties deepSeekProperties;

    /**
     * 配置用于调用 AI 接口的 WebClient
     * 自动注入 API 地址和鉴权 Header
     */
    @Bean
    public WebClient WebClient() {
        return WebClient.builder()
                .baseUrl(deepSeekProperties.getUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + deepSeekProperties.getApiKey())
                .build();
    }
}
