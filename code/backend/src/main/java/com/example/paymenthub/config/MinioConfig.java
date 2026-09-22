package com.example.paymenthub.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình MinioClient.
 * Bean chỉ được tạo khi property "minio.url" được khai báo trong application.yml.
 *
 * Nếu không cấu hình MinIO → Bean không tồn tại → MinioStorageServiceImpl không được inject
 * → FileStorageService sẽ tự động fallback sang LocalStorageServiceImpl.
 */
@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.url:}")
    private String minioUrl;

    @Value("${minio.access-key:}")
    private String accessKey;

    @Value("${minio.secret-key:}")
    private String secretKey;

    /**
     * Bean MinioClient chỉ được tạo khi minio.url được cấu hình và không rộng.
     * Sử dụng SpEL expression để kiểm tra cả tồn tại property lẫn giá trị không rộng.
     * Dev local không cài MinIO (MINIO_URL rộng) → Bean không được tạo → LocalStorageServiceImpl được dùng.
     */
    @Bean
    @ConditionalOnExpression("'${minio.url:}'.length() > 0")
    public MinioClient minioClient() {
        log.info("[MinioConfig] MinIO configured at: {}", minioUrl);
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }
}
