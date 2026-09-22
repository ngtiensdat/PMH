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
public class ExportProgressService {

    private static final String KEY_PREFIX = "export:job:";
    private static final String KEY_SUFFIX = ":progress";
    private static final Duration PROGRESS_TTL = Duration.ofHours(24);
    private static final String SEPARATOR = ":";

    // Fallback khi Redis offline
    private final ConcurrentMap<String, String> localProgressMap = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    public void updateProgress(String jobId, long processedRows, long totalRows) {
        String key = buildKey(jobId);
        String value = processedRows + SEPARATOR + totalRows;
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, value, PROGRESS_TTL);
                return;
            }
        } catch (Exception e) {
            log.warn("[ExportProgressService] Redis unavailable, using in-memory: {}", e.getMessage());
        }
        localProgressMap.put(key, value);
    }

    public long[] getProgress(String jobId) {
        String key = buildKey(jobId);
        String value = null;
        try {
            if (redisTemplate != null) {
                value = redisTemplate.opsForValue().get(key);
            }
        } catch (Exception e) {
            log.warn("[ExportProgressService] Redis read failed, using in-memory: {}", e.getMessage());
            value = localProgressMap.get(key);
        }
        if (value == null) {
            value = localProgressMap.get(key); // fallback
        }

        if (value == null)
            return new long[] { 0, 0 };

        String[] parts = value.split(SEPARATOR, 2);
        if (parts.length < 2)
            return new long[] { 0, 0 };

        try {
            return new long[] { Long.parseLong(parts[0]), Long.parseLong(parts[1]) };
        } catch (NumberFormatException e) {
            return new long[] { 0, 0 };
        }
    }

    /** Xóa cache tiến độ khi job kết thúc (COMPLETED hoặc FAILED). */
    public void clearProgress(String jobId) {
        String key = buildKey(jobId);
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(key);
            }
        } catch (Exception ignored) {
        }
        localProgressMap.remove(key);
    }

    private String buildKey(String jobId) {
        return KEY_PREFIX + jobId + KEY_SUFFIX;
    }
}
