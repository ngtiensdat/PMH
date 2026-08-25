package com.example.paymenthub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${app.security.rate-limit.login-capacity:5}")
    private long loginCapacity;

    @Value("${app.security.rate-limit.login-duration-minutes:1}")
    private long loginDurationMinutes;

    @Value("${app.security.rate-limit.api-capacity:100}")
    private long apiCapacity;

    @Value("${app.security.rate-limit.api-duration-minutes:1}")
    private long apiDurationMinutes;

    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        if (path.startsWith("/api/auth/login")) {
            Bucket bucket = loginBuckets.computeIfAbsent(clientIp, ip -> createLoginBucket());
            if (!bucket.tryConsume(1)) {
                log.warn("[RateLimiting] IP {} vượt quá giới hạn thử đăng nhập (Rate limit {} req/{} min)",
                        clientIp, loginCapacity, loginDurationMinutes);
                sendRateLimitError(response, "Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau!");
                return;
            }
        } else if (path.startsWith("/api/")) {
            Bucket bucket = apiBuckets.computeIfAbsent(clientIp, ip -> createApiBucket());
            if (!bucket.tryConsume(1)) {
                log.warn("[RateLimiting] IP {} vượt quá giới hạn gọi API (Rate limit {} req/{} min)",
                        clientIp, apiCapacity, apiDurationMinutes);
                sendRateLimitError(response, "Bạn đã gửi quá nhiều yêu cầu đến hệ thống. Vui lòng thử lại sau!");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(loginCapacity)
                .refillIntervally(loginCapacity, Duration.ofMinutes(loginDurationMinutes))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(apiCapacity)
                .refillIntervally(apiCapacity, Duration.ofMinutes(apiDurationMinutes))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private void sendRateLimitError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("code", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("message", message);
        body.put("data", null);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
