package com.example.paymenthub.controller;

import com.example.paymenthub.common.base.ApiResponse;
import com.example.paymenthub.common.base.BaseController;
import com.example.paymenthub.dto.request.LoginRequest;
import com.example.paymenthub.dto.response.LoginResponse;
import com.example.paymenthub.security.SecurityUtils;
import com.example.paymenthub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private static final String ACCESS_COOKIE_NAME = "pmh_jwt_token";
    private static final String REFRESH_COOKIE_NAME = "pmh_refresh_token";
    
    private static final long ACCESS_MAX_AGE_SECONDS = 900L;     // 15 phút
    private static final long REFRESH_MAX_AGE_SECONDS = 36000L;  // 10 tiếng

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        
        ResponseCookie accessCookie = createCookie(ACCESS_COOKIE_NAME, response.getToken(), ACCESS_MAX_AGE_SECONDS, "/");
        ResponseCookie refreshCookie = createCookie(REFRESH_COOKIE_NAME, response.getRefreshToken(), REFRESH_MAX_AGE_SECONDS, "/api/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(response, "Đăng nhập thành công!"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest requestBody) {
        
        String refreshTokenStr = cookieRefreshToken != null ? cookieRefreshToken : (requestBody != null ? requestBody.getRefreshToken() : null);
        LoginResponse response = authService.refreshToken(refreshTokenStr);

        ResponseCookie accessCookie = createCookie(ACCESS_COOKIE_NAME, response.getToken(), ACCESS_MAX_AGE_SECONDS, "/");
        ResponseCookie refreshCookie = createCookie(REFRESH_COOKIE_NAME, response.getRefreshToken(), REFRESH_MAX_AGE_SECONDS, "/api/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(response, "Gia hạn phiên làm việc thành công!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = ACCESS_COOKIE_NAME, required = false) String accessCookieToken,
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshCookieToken,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        String accessToken = resolveToken(accessCookieToken, authHeader);
        authService.logout(accessToken, refreshCookieToken);

        ResponseCookie clearAccessCookie = createCookie(ACCESS_COOKIE_NAME, "", 0, "/");
        ResponseCookie clearRefreshCookie = createCookie(REFRESH_COOKIE_NAME, "", 0, "/api/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString())
                .body(ApiResponse.success(null, "Đăng xuất thành công!"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        LoginResponse response = authService.getCurrentUser(username);
        return ok(response, "Lấy thông tin tài khoản thành công!");
    }

    // ─── Inner Helper DTO ──────────────────────────────────────────────
    @lombok.Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }

    // ─── Private Helper Methods ──────────────────────────────────────────────
    private ResponseCookie createCookie(String name, String value, long maxAgeSeconds, String path) {
        return ResponseCookie.from(name, value != null ? value : "")
                .httpOnly(true)
                .secure(false) // Đặt true khi chạy HTTPS trên môi trường Production
                .path(path)
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
    }

    private String resolveToken(String cookieToken, String authHeader) {
        if (cookieToken != null && !cookieToken.trim().isEmpty()) {
            return cookieToken;
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
