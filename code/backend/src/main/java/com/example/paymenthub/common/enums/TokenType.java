package com.example.paymenthub.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum quản lý các loại Token (Access / Refresh Token), tên Cookie và thời gian sống.
 */
@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS("ACCESS", "pmh_jwt_token", 900L),        // 15 phút (900 giây)
    REFRESH("REFRESH", "pmh_refresh_token", 36000L); // 10 tiếng (36.000 giây)

    private final String typeName;
    private final String cookieName;
    private final long maxAgeSeconds;

    public long getExpirationMs() {
        return maxAgeSeconds * 1000L;
    }
}
