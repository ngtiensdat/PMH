package com.example.paymenthub.service.impl;

import com.example.paymenthub.service.FileStorageService;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * MinIO implementation của FileStorageService.
 * Được khởi tạo bởi StorageConfig khi MinioClient bean tồn tại.
 * Người dùng tải file trực tiếp qua Presigned URL (không stream qua Spring Boot).
 */
@Slf4j
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;
    private final String bucketName;
    private volatile boolean bucketChecked = false;

    /** Thư mục gốc trong bucket — tất cả export file đều nằm trong prefix này */
    private static final String OBJECT_PREFIX = "exports/";

    /** Presigned URL tối đa 12 giờ (MaxExpiry của MinIO là 7 ngày, nhưng job chỉ valid 12h) */
    private static final int MAX_PRESIGNED_SECONDS = 43200; // 12 giờ

    @Override
    public String upload(String fileName, InputStream data, long size) throws Exception {
        ensureBucketExists();

        String objectKey = OBJECT_PREFIX + java.time.LocalDate.now() + "/" + fileName;
        String contentType = resolveContentType(fileName);

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(data, size, -1)
                .contentType(contentType)
                .build());

        log.info("[MinIO] Uploaded: bucket={}, key={}, contentType={}", bucketName, objectKey, contentType);
        return objectKey;
    }

    @Override
    public String generateDownloadUrl(String filePath, LocalDateTime expiresAt) throws Exception {
        long secondsUntilExpiry = ChronoUnit.SECONDS.between(LocalDateTime.now(), expiresAt);
        if (secondsUntilExpiry <= 0) {
            throw new IllegalStateException("File đã hết hạn");
        }

        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(filePath)
                .expiry((int) Math.min(secondsUntilExpiry, MAX_PRESIGNED_SECONDS), TimeUnit.SECONDS)
                .build());
    }

    private void ensureBucketExists() throws Exception {
        if (bucketChecked) return;
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("[MinIO] Bucket created: {}", bucketName);
        }
        bucketChecked = true;
    }

    /**
     * Suy diễn Content-Type từ phần mở rộng của tên file.
     * Mặc định trả về "application/octet-stream" nếu không nhận dạng được.
     */
    private String resolveContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (lower.endsWith(".csv")) {
            return "text/csv; charset=UTF-8";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    @Override
    public void delete(String filePath) throws Exception {
        if (filePath == null || filePath.isBlank()) return;
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(filePath)
                .build());
        log.info("[MinIO] Deleted object: bucket={}, key={}", bucketName, filePath);
    }
}
