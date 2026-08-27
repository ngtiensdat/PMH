package com.example.paymenthub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtProvider {

    private static final String SECRET_STRING = "PaymentHubEnterpriseSecretKeyForJWTAuthenticationAndAuthorizationSystem2026";
    
    public static final long ACCESS_TOKEN_EXPIRATION_MS = 900000L;    // 15 phút
    public static final long REFRESH_TOKEN_EXPIRATION_MS = 36000000L; // 10 tiếng

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    public String generateAccessToken(String username, String role, List<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION_MS);

        String permString = permissions != null ? String.join(",", permissions) : "";

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("permissions", permString)
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION_MS);

        return Jwts.builder()
                .subject(username)
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateToken(String username, String role) {
        return generateAccessToken(username, role, List.of());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    public String getTypeFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("type", String.class);
        } catch (Exception e) {
            log.error("[JWT] Không thể đọc claim type từ Token: {}", e.getMessage());
            return null;
        }
    }

    public List<String> getPermissionsFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String permString = claims.get("permissions", String.class);
            if (permString != null && !permString.trim().isEmpty()) {
                return Arrays.asList(permString.split(","));
            }
        } catch (Exception e) {
            log.error("[JWT] Không thể đọc permissions từ Token: {}", e.getMessage());
        }
        return List.of();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("[JWT] Token không hợp lệ hoặc đã hết hạn: {}", e.getMessage());
        }
        return false;
    }

    public Date getExpirationFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration();
        } catch (Exception e) {
            return new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_MS);
        }
    }
}
