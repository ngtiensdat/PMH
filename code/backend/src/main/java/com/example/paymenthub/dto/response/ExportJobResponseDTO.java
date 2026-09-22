package com.example.paymenthub.dto.response;

import com.example.paymenthub.common.enums.ExportJobStatus;
import com.example.paymenthub.entity.ExportJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO trả về thông tin Export Job cho Frontend.
 * Dùng cho: polling tiến độ, danh sách my-jobs, và phản hồi tạo job.
 *
 * jobId = ExportJob.id (Long) — số tự tăng từ DB, dùng làm định danh trong API URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobResponseDTO {

    private Long            jobId;          // = ExportJob.id (NUMBER PK)
    private ExportJobStatus status;
    private long            processedRows;
    private Long            totalRows;
    private int             progressPercent;    // 0–100, tính từ processedRows/totalRows
    private String          fileName;           // Tên file gốc để hiển thị trên UI
    private String          downloadUrl;        // MinIO Presigned URL hoặc null nếu chưa COMPLETED
    private String          errorMessage;
    private LocalDateTime   createdAt;
    private LocalDateTime   completedAt;        // Ánh xạ từ ExportJob.updatedAt khi DONE
    private LocalDateTime   expiresAt;
    private int             downloadCount;
    private boolean         isRead;
    private boolean         isFileDeleted;

    /** Factory method: từ Entity + URL đã tính sẵn bên ngoài. */
    public static ExportJobResponseDTO fromEntity(ExportJob job, String downloadUrl) {
        int percent = 0;
        if (job.getTotalRows() != null && job.getTotalRows() > 0) {
            percent = (int) Math.min(100, job.getProcessedRows() * 100L / job.getTotalRows());
        }

        // Dùng ExportJobStatus.isSuccess() để kiểm tra trạng thái "thành công"
        // — tập trung logic tại 1 chỗ, tương thích với dữ liệu cũ (SUCCESS, COMPLETED)
        ExportJobStatus statusEnum;
        try {
            statusEnum = ExportJobStatus.valueOf(job.getStatus());
        } catch (IllegalArgumentException e) {
            statusEnum = ExportJobStatus.FAILED; // fallback an toàn nếu DB có giá trị lạ
        }

        LocalDateTime completedAt = statusEnum.isSuccess() ? job.getUpdatedAt() : null;
        boolean fileDeleted = statusEnum.isSuccess() && (job.getExportUrl() == null || job.getExportUrl().isBlank());

        return ExportJobResponseDTO.builder()
                .jobId(job.getId())
                .status(statusEnum)
                .processedRows(job.getProcessedRows())
                .totalRows(job.getTotalRows())
                .progressPercent(percent)
                .fileName(job.getFileName())
                .downloadUrl(downloadUrl)
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .completedAt(completedAt)
                .expiresAt(job.getExpiresAt())
                .downloadCount(job.getDownloadCount())
                .isRead(job.getDownloadCount() > 0)
                .isFileDeleted(fileDeleted)
                .build();
    }

    /** Factory method không kèm URL (dùng khi job chưa DONE). */
    public static ExportJobResponseDTO fromEntity(ExportJob job) {
        return fromEntity(job, null);
    }
}
