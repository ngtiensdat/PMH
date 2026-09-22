package com.example.paymenthub.scheduler;

import com.example.paymenthub.entity.ExportJob;
import com.example.paymenthub.repository.ExportJobRepository;
import com.example.paymenthub.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler tự động dọn dẹp file vật lý trên MinIO khi hết hạn.
 * Quét mỗi 2 phút: Tìm các job có expiresAt <= NOW() và exportUrl khác null.
 * Thực hiện xóa file binary trên MinIO để tiết kiệm bộ nhớ đĩa,
 * đồng thời cập nhật exportUrl = null để giữ lại lịch sử Audit Log trong Oracle
 * DB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportMinioCleanupScheduler {

    private final ExportJobRepository exportJobRepository;
    private final FileStorageService fileStorageService;

    @Scheduled(fixedDelay = 2 * 60 * 1000) // Chạy mỗi 2 phút
    @SchedulerLock(name = "ExportMinioCleanupScheduler_cleanExpiredMinioFiles", lockAtMostFor = "PT1M", lockAtLeastFor = "PT10S")
    @Transactional
    public void cleanExpiredMinioFiles() {
        LocalDateTime now = LocalDateTime.now();
        List<ExportJob> expiredJobs = exportJobRepository.findExpiredJobsWithFile(now);

        if (expiredJobs.isEmpty()) {
            return;
        }

        log.info("[ExportMinioCleanupScheduler] Phát hiện {} file xuất đã hết hạn. Đang dọn dẹp MinIO...",
                expiredJobs.size());

        for (ExportJob job : expiredJobs) {
            String path = job.getExportUrl();
            if (path != null && !path.isBlank()) {
                try {
                    fileStorageService.delete(path);
                    log.info("[ExportMinioCleanupScheduler] Đã xóa file MinIO: jobId={}, path={}", job.getId(), path);
                } catch (Exception e) {
                    log.warn("[ExportMinioCleanupScheduler] Không thể xóa file MinIO cho jobId={}: {}", job.getId(),
                            e.getMessage());
                }
            }
            // Đặt exportUrl = null để đánh dấu file đĩa đã được xóa, nhưng vẫn giữ nguyên
            // bản ghi Audit Log trong DB
            job.setExportUrl(null);
            job.setUpdatedAt(now);
        }

        exportJobRepository.saveAll(expiredJobs);
    }
}
