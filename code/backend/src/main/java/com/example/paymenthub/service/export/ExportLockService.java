package com.example.paymenthub.service.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class ExportLockService {

    private static final String KEY_PREFIX = "export:user:";
    private static final String KEY_SUFFIX = ":active_job";
    private static final Duration LOCK_TTL = Duration.ofHours(2);

    // Fallback khi Redis offline
    private final ConcurrentMap<String, Boolean> localLockMap = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    public boolean tryLock(String userId) {
        String key = buildKey(userId);
        try {
            if (redisTemplate != null) {
                Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL);
                return Boolean.TRUE.equals(locked);
            }
        } catch (Exception e) {
            log.warn("[ExportLockService] Redis unavailable, using in-memory lock: {}", e.getMessage());
        }
        // Fallback: ConcurrentHashMap (an toàn với concurrent access)
        return localLockMap.putIfAbsent(userId, Boolean.TRUE) == null;
    }

    public void unlock(String userId) {
        String key = buildKey(userId);
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(key);
                return;
            }
        } catch (Exception e) {
            log.warn("[ExportLockService] Redis unavailable, removing in-memory lock: {}", e.getMessage());
        }
        localLockMap.remove(userId);
    }

    private String buildKey(String userId) {
        return KEY_PREFIX + userId + KEY_SUFFIX;
    }
}
