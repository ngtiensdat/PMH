package com.example.paymenthub.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TokenBlacklistService {

    // Map lưu Token bị thu hồi kèm theo thời gian hết hạn của Token đó (Epoch Milliseconds)
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Đưa Token vào danh sách đen (Blacklist) khi người dùng Đăng xuất hoặc bị khóa tài khoản.
     */
    public void blacklistToken(String token, long expirationTimeMs) {
        if (token != null && !token.trim().isEmpty()) {
            blacklistedTokens.put(token, expirationTimeMs);
            log.info("[TokenBlacklist] Đã thu hồi thành công Token.");
        }
    }

    /**
     * Kiểm tra xem Token có nằm trong danh sách đen bị thu hồi hay không.
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        Long expiry = blacklistedTokens.get(token);
        if (expiry == null) {
            return false;
        }
        // Nếu Token đã quá thời gian hết hạn tự nhiên thì không cần giữ lại nữa
        if (System.currentTimeMillis() > expiry) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }

    /**
     * Đã bật @EnableScheduling: Tự động dọn dẹp các Token đã hết hạn tự nhiên khỏi RAM mỗi 30 phút.
     */
    @Scheduled(fixedRate = 1800000) // 30 phút
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        int initialSize = blacklistedTokens.size();
        blacklistedTokens.entrySet().removeIf(entry -> now > entry.getValue());
        int removed = initialSize - blacklistedTokens.size();
        if (removed > 0) {
            log.info("[TokenBlacklist] Đã dọn dẹp {} Token hết hạn khỏi bộ nhớ RAM.", removed);
        }
    }
}
