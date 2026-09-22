package com.example.paymenthub.config;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cấu hình ShedLock — đảm bảo chỉ 1 instance chạy Scheduler tại một thời điểm.
 * Quan trọng khi deploy multi-instance (Docker Swarm / Kubernetes).
 *
 * ShedLock dùng Redis làm Lock Store (cùng infrastructure với Export Progress Cache).
 * Fallback: Nếu Redis không available, @SchedulerLock sẽ không block — Scheduler vẫn chạy
 * nhưng không đảm bảo distributed locking (chấp nhận được trong môi trường single-instance dev).
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(@Autowired(required = false) RedisConnectionFactory connectionFactory) {
        if (connectionFactory != null) {
            log.info("[ShedLockConfig] Using Redis as ShedLock provider");
            return new RedisLockProvider(connectionFactory);
        }
        // Fallback: trả về NoOp LockProvider khi không có Redis
        // (chấp nhận được trong môi trường single-instance dev local)
        log.warn("[ShedLockConfig] Redis not available. ShedLock will use NoOp provider (no distributed locking).");
        return lockConfiguration -> java.util.Optional.empty();
    }
}
