package com.example.paymenthub.scheduler;

import com.example.paymenthub.common.enums.ExportJobStatus;
import com.example.paymenthub.entity.ExportJob;
import com.example.paymenthub.repository.ExportJobRepository;
import com.example.paymenthub.service.export.ExportLockService;
import com.example.paymenthub.service.export.ExportProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Scheduler tự động phát hiện và phục hồi các Export Job bị kẹt.
 *
 * Kịch bản cần xử lý:
 *   - Worker bị crash giữa chừng → job mãi ở trạng thái PROCESSING
 *   - Server restart → job mãi ở trạng thái PENDING hoặc PROCESSING
 *   - Heartbeat không được cập nhật trong > 5 phút → job được coi là stuck
 *
 * ShedLock: Đảm bảo chỉ 1 instance chạy Scheduler tại một thời điểm
 * (quan trọng khi scale multi-instance trong môi trường Kubernetes/Docker Swarm).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportRecoveryScheduler {

    /** Ngưỡng thời gian không có heartbeat để coi là stuck job (5 phút) */
    private static final long STUCK_THRESHOLD_MINUTES = 5;

    private final ExportJobRepository exportJobRepository;
    private final ExportLockService exportLockService;
    private final ExportProgressService exportProgressService;

    /**
     * Chạy mỗi 5 phút. Quét tìm job bị stuck và đánh dấu FAILED.
     * @SchedulerLock: Lock ShedLock trong tối đa 4 phút (nhỏ hơn interval 5 phút để tránh deadlock).
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000) // 5 phút
    @SchedulerLock(name = "ExportRecoveryScheduler_recoverStuckJobs",
                   lockAtMostFor = "PT4M",
                   lockAtLeastFor = "PT1M")
    @Transactional
    public void recoverStuckJobs() {
        LocalDateTime heartbeatThreshold = LocalDateTime.now().minusMinutes(STUCK_THRESHOLD_MINUTES);
        List<ExportJob> stuckJobs = exportJobRepository.findStuckJobs(heartbeatThreshold);

        if (stuckJobs.isEmpty()) {
            log.debug("[ExportRecoveryScheduler] No stuck jobs found.");
            return;
        }

        log.warn("[ExportRecoveryScheduler] Found {} stuck job(s). Marking as FAILED...", stuckJobs.size());

        for (ExportJob job : stuckJobs) {
            job.setStatus(ExportJobStatus.FAILED.name());
            job.setErrorMessage("Job tự động hủy do không có phản hồi sau " + STUCK_THRESHOLD_MINUTES + " phút (Worker bị crash hoặc Server restart).");
            job.setLastHeartbeatAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());

            // Giải phóng User Lock và xóa cache tiến độ
            exportLockService.unlock(String.valueOf(job.getUserId()));
            exportProgressService.clearProgress(String.valueOf(job.getId()));

            log.warn("[ExportRecoveryScheduler] Job FAILED (stuck): id={}, userId={}", job.getId(), job.getUserId());
        }

        // Lưu tất cả trong một lần — giảm từ N round-trip xuống 1 lần DB write
        exportJobRepository.saveAll(stuckJobs);
    }

    /**
     * Cron Job chạy mỗi 1 tiếng (phút 00 hàng giờ):
     * Quét các bản ghi có trạng thái (PENDING, PROCESSING, FAILED) mà thời gian cập nhật lần cuối
     * (updatedAt / lastHeartbeatAt) đã quá 1 tiếng để tự động dọn dẹp và giải phóng lock.
     */
    @Scheduled(cron = "0 0 * * * *") // Chạy vào đầu mỗi giờ (ví dụ 01:00, 02:00, 03:00...)
    @SchedulerLock(name = "ExportRecoveryScheduler_cleanHourlyInactiveJobs",
                   lockAtMostFor = "PT50M",
                   lockAtLeastFor = "PT1M")
    @Transactional
    public void cleanHourlyInactiveJobs() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<ExportJob> inactiveJobs = exportJobRepository.findInactiveJobsByStatuses(oneHourAgo);

        if (inactiveJobs.isEmpty()) {
            log.debug("[ExportRecoveryScheduler] Không có bản ghi PENDING/PROCESSING/FAILED nào ngưng cập nhật > 1 tiếng.");
            return;
        }

        log.info("[ExportRecoveryScheduler] Tìm thấy {} bản ghi ngưng cập nhật > 1 tiếng. Đang giải phóng...", inactiveJobs.size());

        for (ExportJob job : inactiveJobs) {
            // Nếu là PENDING hoặc PROCESSING (chưa kết thúc) → Chuyển thành FAILED
            if (ExportJobStatus.PENDING.name().equals(job.getStatus()) || ExportJobStatus.PROCESSING.name().equals(job.getStatus())) {
                job.setStatus(ExportJobStatus.FAILED.name());
                job.setErrorMessage("Job tự động hủy do không có phản hồi/cập nhật sau 1 tiếng.");
                job.setUpdatedAt(LocalDateTime.now());
                job.setLastHeartbeatAt(LocalDateTime.now());
            }

            // Giải phóng Per-User Lock và clear Redis progress cache
            exportLockService.unlock(String.valueOf(job.getUserId()));
            exportProgressService.clearProgress(String.valueOf(job.getId()));

            log.info("[ExportRecoveryScheduler] Đã dọn dẹp job #{} (User #{}), status={}", job.getId(), job.getUserId(), job.getStatus());
        }

        // Lưu tất cả trong một lần — giảm từ N round-trip xuống 1 lần DB write
        exportJobRepository.saveAll(inactiveJobs);
    }

    /**
     * Kích hoạt NGAY LẬP TỨC khi máy chủ Backend vừa khởi động lại xong.
     * Mọi Job đang PENDING hoặc PROCESSING từ đợt chạy trước đều bị dừng do Java Thread đã bị ngắt.
     * Phương thức này quét và chuyển tất cả về FAILED ngay lập tức mà không cần chờ 5 phút.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationStartup() {
        log.info("[ExportRecoveryScheduler] Máy chủ vừa khởi động lại! Đang quét các Job bị ngắt đột ngột...");
        List<ExportJob> activeJobs = exportJobRepository.findAllActiveJobs();

        if (activeJobs.isEmpty()) {
            log.info("[ExportRecoveryScheduler] Không có Job nào bị dở dang từ đợt chạy trước.");
            return;
        }

        log.warn("[ExportRecoveryScheduler] Phát hiện {} Job bị dở dang do Server restart. Đang chuyển thành FAILED...", activeJobs.size());

        for (ExportJob job : activeJobs) {
            job.setStatus(ExportJobStatus.FAILED.name());
            job.setErrorMessage("Tiến trình bị ngắt do máy chủ (Server) khởi động lại. Vui lòng bấm 'Thử lại'.");
            job.setLastHeartbeatAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());

            // Giải phóng Per-User Lock và clear Redis progress cache
            exportLockService.unlock(String.valueOf(job.getUserId()));
            exportProgressService.clearProgress(String.valueOf(job.getId()));

            log.warn("[ExportRecoveryScheduler] Đã hủy Job dở dang: id={}, userId={}", job.getId(), job.getUserId());
        }

        exportJobRepository.saveAll(activeJobs);
    }
}

