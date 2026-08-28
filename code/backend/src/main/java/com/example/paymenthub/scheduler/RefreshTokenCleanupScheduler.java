package com.example.paymenthub.scheduler;

import com.example.paymenthub.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Tự động dọn dẹp các Refresh Token hết hạn hoặc bị thu hồi (REVOKED = 1)
     * Chạy định kỳ vào 02:00 AM vào ngày cuối cùng của mỗi tháng (cron = "0 0 2 L * ?")
     */
    @Scheduled(cron = "0 0 2 L * ?")
    @Transactional
    public void cleanupMonthlyExpiredTokens() {
        try {
            log.info("[RefreshTokenCleanup] Bắt đầu dọn dẹp Refresh Token hết hạn / thu hồi vào ngày cuối cùng của tháng...");
            int deletedCount = refreshTokenRepository.deleteExpiredOrRevokedTokens(LocalDateTime.now());
            log.info("[RefreshTokenCleanup] Dọn dẹp hoàn tất! Đã xóa {} bản ghi token rác khỏi CSDL.", deletedCount);
        } catch (Exception e) {
            log.error("[RefreshTokenCleanup] Lỗi trong quá trình dọn dẹp token: {}", e.getMessage(), e);
        }
    }
}
