package com.example.paymenthub.repository;

import com.example.paymenthub.entity.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {

    /**
     * Tìm job đang chạy (PENDING hoặc PROCESSING) của user.
     * userId là Long (PMH_APP_USERS.ID) — khớp với cột USER_ID (NUMBER) trên DB.
     * Dùng để kiểm tra Per-User Lock trước khi tạo job mới.
     */
    @Query("SELECT j FROM ExportJob j WHERE j.userId = :userId AND j.status IN ('PENDING', 'PROCESSING')")
    Optional<ExportJob> findActiveJobByUserId(@Param("userId") Long userId);

    /**
     * Lấy danh sách job trong 12 tiếng gần nhất của user (cho Header Notification
     * Bell).
     * Sắp xếp mới nhất lên trên.
     */
    @Query("SELECT j FROM ExportJob j WHERE j.userId = :userId AND j.createdAt >= :since ORDER BY j.createdAt DESC")
    List<ExportJob> findRecentJobsByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Tìm các job PENDING/PROCESSING bị stuck — heartbeat quá cũ.
     * Dùng bởi ExportRecoveryScheduler để auto-detect crash.
     *
     * @param heartbeatThreshold mốc thời gian (ví dụ: now - 5 phút)
     */
    @Query("SELECT j FROM ExportJob j WHERE j.status IN ('PENDING', 'PROCESSING') AND (j.lastHeartbeatAt IS NULL OR j.lastHeartbeatAt < :heartbeatThreshold OR j.createdAt < :heartbeatThreshold)")
    List<ExportJob> findStuckJobs(@Param("heartbeatThreshold") LocalDateTime heartbeatThreshold);

    /**
     * Lấy tất cả job ở trạng thái PENDING hoặc PROCESSING (dùng khi Server vừa khởi động lại).
     */
    @Query("SELECT j FROM ExportJob j WHERE j.status IN ('PENDING', 'PROCESSING')")
    List<ExportJob> findAllActiveJobs();

    /**
     * Tìm các job ở trạng thái (PENDING, PROCESSING, FAILED) ngưng cập nhật > 1 tiếng.
     * Dùng bởi Cronjob quét hàng giờ.
     */
    @Query("SELECT j FROM ExportJob j WHERE j.status IN ('PENDING', 'PROCESSING', 'FAILED') AND (j.updatedAt IS NULL OR j.updatedAt < :threshold)")
    List<ExportJob> findInactiveJobsByStatuses(@Param("threshold") LocalDateTime threshold);

    /**
     * Tìm các job đã hết hạn (expiresAt <= now) mà file vẫn còn trên MinIO (exportUrl khác null).
     * Dùng bởi ExportMinioCleanupScheduler để dọn dẹp đĩa MinIO.
     */
    @Query("SELECT j FROM ExportJob j WHERE j.expiresAt IS NOT NULL AND j.expiresAt <= :now AND j.exportUrl IS NOT NULL AND j.exportUrl <> ''")
    List<ExportJob> findExpiredJobsWithFile(@Param("now") LocalDateTime now);
}
