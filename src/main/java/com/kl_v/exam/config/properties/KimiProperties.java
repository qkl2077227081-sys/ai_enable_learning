package com.kl_v.exam.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ClassName: KimiProperties
 * Package: com.kl_v.exam.config.properties
 * Description:
 *
 * @Author V
 * @Create 2026/4/24 下午7:04
 * @Version 1.0
 */

@ConfigurationProperties(prefix = "kimi.api")
@Data
public class KimiProperties {
    private String model;
    private String url;
    private String apiKey;
    private Integer maxTokens;
    private Double temperature;
}
