package com.example.paymenthub.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class ActiveStatusScheduler {

    private final JdbcTemplate jdbcTemplate;

    public ActiveStatusScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Tự động quét và cập nhật trạng thái IS_ACTIVE mỗi 5 giây (fixedRate = 5000ms)
     * dựa theo thời gian thực (SYSDATE của Oracle DB) so với EFFECTIVE_DATE và
     * END_EFFECTIVE_DATE.
     */
    // @Scheduled(fixedRate = 5000)
    @Transactional
    public void updateActiveStatus() {
        try {
            // ─── 1. BẢNG PMH_GROUP_CATEGORY ──────────────────────────────────────────
            // Gộp cả 2 luồng Bật (1) và Tắt (0) thành 1 câu SQL duy nhất bằng CASE WHEN
            String sqlCat = "UPDATE PMH_GROUP_CATEGORY " +
                    "SET IS_ACTIVE = CASE " +
                    "    WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1 "
                    +
                    "    ELSE 0 " +
                    "END " +
                    "WHERE IS_ACTIVE <> ( " +
                    "    CASE " +
                    "        WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1 "
                    +
                    "        ELSE 0 " +
                    "    END " +
                    ")";
            int catUpdated = jdbcTemplate.update(sqlCat);

            // ─── 2. BẢNG PMH_COMPONENTS ──────────────────────────────────────────────
            String sqlComp = "UPDATE PMH_COMPONENTS " +
                    "SET IS_ACTIVE = CASE " +
                    "    WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1 "
                    +
                    "    ELSE 0 " +
                    "END " +
                    "WHERE IS_ACTIVE <> ( " +
                    "    CASE " +
                    "        WHEN EFFECTIVE_DATE <= SYSDATE AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) THEN 1 "
                    +
                    "        ELSE 0 " +
                    "    END " +
                    ")";
            int compUpdated = jdbcTemplate.update(sqlComp);

            if (catUpdated > 0 || compUpdated > 0) {
                log.info("[ActiveStatusScheduler] Updated IS_ACTIVE. Categories updated: {}, Components updated: {}",
                        catUpdated, compUpdated);
            }
        } catch (Exception e) {
            log.error("[ActiveStatusScheduler] Error updating active status: {}", e.getMessage(), e);
        }
    }
}
