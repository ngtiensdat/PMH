package com.example.paymenthub.service;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * Interface trừu tượng hóa việc lưu trữ file CSV kết quả export.
 * Có 2 cài đặt:
 *   - MinioStorageServiceImpl: dùng khi MinIO được cấu hình (production)
 *   - LocalStorageServiceImpl: fallback khi không có MinIO (dev local)
 */
public interface FileStorageService {

    /**
     * Upload file lên hệ thống lưu trữ.
     *
     * @param fileName  tên file (VD: "Transaction_Log_20260916_uuid.csv")
     * @param data      InputStream của file CSV
     * @param size      kích thước file (bytes), -1 nếu không biết
     * @return đường dẫn lưu trữ (MinIO Object Key hoặc path local) — dùng để điền vào EXPORT_JOB.FILE_PATH
     */
    String upload(String fileName, InputStream data, long size) throws Exception;

    /**
     * Sinh URL tải file có thời hạn.
     * MinIO: Presigned URL (browser tải thẳng từ MinIO, không qua Spring Boot).
     * Local: URL endpoint nội bộ /api/transaction-log/export-jobs/{jobId}/file.
     *
     * @param filePath  đường dẫn trả về từ upload()
     * @param expiresAt thời điểm hết hạn URL (= EXPIRES_AT của job)
     * @return URL tải file
     */
    String generateDownloadUrl(String filePath, LocalDateTime expiresAt) throws Exception;

    /**
     * Xóa file khỏi hệ thống lưu trữ (MinIO / Local).
     *
     * @param filePath đường dẫn trả về từ upload()
     */
    void delete(String filePath) throws Exception;
}
