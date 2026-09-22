package com.example.paymenthub.service.impl;

import com.example.paymenthub.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Local filesystem fallback cho FileStorageService.
 * Được khởi tạo bởi StorageConfig khi MinIO không được cấu hình.
 * File lưu vào ./exports/, URL là endpoint nội bộ Spring Boot.
 */
@Slf4j
public class LocalStorageServiceImpl implements FileStorageService {

    private static final String EXPORT_DIR = "./exports";
    private static final int BUFFER_SIZE = 8192;

    /**
     * Đường dẫn endpoint nội bộ Spring Boot để tải file local.
     * Phải khớp với mapping trong TransactionLogController.downloadLocalFile().
     */
    private static final String DOWNLOAD_BASE_PATH = "/api/downloads/exports/";

    @Override
    public String upload(String fileName, InputStream data, long size) throws Exception {
        Path exportDir = Paths.get(EXPORT_DIR);
        if (!Files.exists(exportDir)) {
            Files.createDirectories(exportDir);
        }

        Path targetPath = exportDir.resolve(fileName);
        try (OutputStream out = Files.newOutputStream(targetPath)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = data.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        log.info("[LocalStorage] File saved: {}", targetPath.toAbsolutePath());
        return fileName;
    }

    @Override
    public String generateDownloadUrl(String filePath, LocalDateTime expiresAt) {
        return DOWNLOAD_BASE_PATH + filePath;
    }

    @Override
    public void delete(String filePath) throws Exception {
        if (filePath == null || filePath.isBlank()) return;
        Path targetPath = Paths.get(EXPORT_DIR).resolve(filePath);
        Files.deleteIfExists(targetPath);
        log.info("[LocalStorage] File deleted: {}", targetPath.toAbsolutePath());
    }
}
