package com.example.paymenthub.config;

import com.example.paymenthub.service.FileStorageService;
import com.example.paymenthub.service.impl.LocalStorageServiceImpl;
import com.example.paymenthub.service.impl.MinioStorageServiceImpl;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quyết định implementation nào của FileStorageService được dùng.
 *
 * Quy tắc:
 *   - MinioStorageServiceImpl : nếu MinioClient bean tồn tại (minio.url được cấu hình và không rỗng)
 *   - LocalStorageServiceImpl : fallback khi MinIO không được cấu hình (dev local)
 *
 * Dùng @Configuration + @Bean thay vì @ConditionalOnMissingBean trên @Service class
 * để Spring Boot đảm bảo thứ tự evaluation đúng.
 */
@Slf4j
@Configuration
public class StorageConfig {

    @Value("${minio.bucket-name:export-logs}")
    private String bucketName;

    /**
     * MinIO implementation — chỉ active khi MinioClient Bean tồn tại.
     * MinioClient chỉ được tạo khi minio.url != "" (xem MinioConfig).
     */
    @Bean
    @ConditionalOnBean(MinioClient.class)
    public FileStorageService minioStorageService(MinioClient minioClient) {
        log.info("[StorageConfig] Using MinIO storage (bucket={})", bucketName);
        return new MinioStorageServiceImpl(minioClient, bucketName);
    }

    /**
     * Local filesystem fallback — active khi KHÔNG có FileStorageService nào được tạo.
     * File lưu vào ./exports/, URL là endpoint nội bộ Spring Boot.
     */
    @Bean
    @ConditionalOnMissingBean(FileStorageService.class)
    public FileStorageService localStorageService() {
        log.info("[StorageConfig] MinIO not configured. Using local file storage (./exports/)");
        return new LocalStorageServiceImpl();
    }
}
