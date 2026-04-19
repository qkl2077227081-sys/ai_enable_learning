package com.kl_v.exam.config;

import com.kl_v.exam.config.properties.MinioProperties;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: MinioConfiguration
 * Package: com.kl_v.exam.config
 * Description:
 *
 * @Author V
 * @Create 2026/4/19 下午2:15
 * @Version 1.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfiguration {
    @Autowired
    private MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient(){
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getUsername(), minioProperties.getPassword())
                .build();
        log.info("完成了minio的链接和配置！minio的客户端对象为：{}",minioClient);
        return minioClient;
    }

}
