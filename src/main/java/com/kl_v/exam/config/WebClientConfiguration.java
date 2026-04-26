package com.kl_v.exam.config;

import com.kl_v.exam.config.properties.KimiProperties;
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
@EnableConfigurationProperties(KimiProperties.class)
@Configuration
public class WebClientConfiguration {
    @Autowired
    private KimiProperties kimiProperties;
    @Bean
    public WebClient webClient(){
        WebClient webClient = WebClient.builder()
                .baseUrl(kimiProperties.getUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization","Bearer " +kimiProperties.getApiKey())
                .build();
        return webClient;
    }
}
