package com.kl_v.exam.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ClassName: DeepseekProperties
 * Package: com.kl_v.exam.config.properties
 * Description:
 *
 * @Author V
 * @Create 2026/5/1 下午5:30
 * @Version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "deepseek.api")
public class DeepSeekProperties {
    /**
     * 模型名称，如 deepseek-chat
     */
    private String model;

    /**
     * API 请求地址
     */
    private String url;

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 最大生成 Token 数
     */
    private Integer maxTokens;

    /**
     * 生成采样温度 (0.0 - 2.0)
     */
    private Double temperature;
}
