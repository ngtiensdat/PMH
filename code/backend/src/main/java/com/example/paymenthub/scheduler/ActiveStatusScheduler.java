package com.example.paymenthub.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
     * dựa theo thời gian thực (SYSDATE của Oracle DB) so với EFFECTIVE_DATE và END_EFFECTIVE_DATE.
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void updateActiveStatus() {
        try {
            // ─── 1. BẢNG PMH_GROUP_CATEGORY ──────────────────────────────────────────
            // Kích hoạt = 1 nếu: Đã đến ngày hiệu lực VÀ (Chưa có ngày hết hạn HOẶC Chưa đến ngày hết hạn) VÀ IS_ACTIVE đang là 0
            String sqlCatActive = 
                "UPDATE PMH_GROUP_CATEGORY " +
                "SET IS_ACTIVE = 1 " +
                "WHERE EFFECTIVE_DATE <= SYSDATE " +
                "  AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) " +
                "  AND IS_ACTIVE = 0";
            int catActivated = jdbcTemplate.update(sqlCatActive);

            // Vô hiệu hóa = 0 nếu: Chưa đến ngày hiệu lực HOẶC Đã quá ngày hết hạn VÀ IS_ACTIVE đang là 1
            String sqlCatInactive = 
                "UPDATE PMH_GROUP_CATEGORY " +
                "SET IS_ACTIVE = 0 " +
                "WHERE (EFFECTIVE_DATE > SYSDATE OR (END_EFFECTIVE_DATE IS NOT NULL AND END_EFFECTIVE_DATE < SYSDATE)) " +
                "  AND IS_ACTIVE = 1";
            int catDeactivated = jdbcTemplate.update(sqlCatInactive);

            // ─── 2. BẢNG PMH_COMPONENTS ──────────────────────────────────────────────
            // Kích hoạt = 1
            String sqlCompActive = 
                "UPDATE PMH_COMPONENTS " +
                "SET IS_ACTIVE = 1 " +
                "WHERE EFFECTIVE_DATE <= SYSDATE " +
                "  AND (END_EFFECTIVE_DATE IS NULL OR END_EFFECTIVE_DATE >= SYSDATE) " +
                "  AND IS_ACTIVE = 0";
            int compActivated = jdbcTemplate.update(sqlCompActive);

            // Vô hiệu hóa = 0
            String sqlCompInactive = 
                "UPDATE PMH_COMPONENTS " +
                "SET IS_ACTIVE = 0 " +
                "WHERE (EFFECTIVE_DATE > SYSDATE OR (END_EFFECTIVE_DATE IS NOT NULL AND END_EFFECTIVE_DATE < SYSDATE)) " +
                "  AND IS_ACTIVE = 1";
            int compDeactivated = jdbcTemplate.update(sqlCompInactive);

            if (catActivated > 0 || catDeactivated > 0 || compActivated > 0 || compDeactivated > 0) {
                log.info("[ActiveStatusScheduler] Updated IS_ACTIVE. Categories: +{} / -{}, Components: +{} / -{}",
                        catActivated, catDeactivated, compActivated, compDeactivated);
            }
        } catch (Exception e) {
            log.error("[ActiveStatusScheduler] Error updating active status: {}", e.getMessage(), e);
        }
    }
}
