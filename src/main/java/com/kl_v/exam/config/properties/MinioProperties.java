package com.kl_v.exam.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: MinioProperties
 * Package: com.kl_v.exam.config.properties
 * Description:
 *
 * @Author V
 * @Create 2026/4/19 下午1:56
 * @Version 1.0
 */

@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {
    private String endpoint;
    private String username;
    private String password;
    private String bucketName;
}
