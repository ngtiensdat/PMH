package com.example.paymenthub.controller;

import com.example.paymenthub.common.base.ApiResponse;
import com.example.paymenthub.common.base.BaseController;
import com.example.paymenthub.common.enums.TokenType;
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

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        
        ResponseCookie accessCookie = createCookie(TokenType.ACCESS.getCookieName(), response.getToken(), TokenType.ACCESS.getMaxAgeSeconds(), "/");
        ResponseCookie refreshCookie = createCookie(TokenType.REFRESH.getCookieName(), response.getRefreshToken(), TokenType.REFRESH.getMaxAgeSeconds(), "/api/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(response, "Đăng nhập thành công!"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @CookieValue(name = "pmh_refresh_token", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest requestBody) {
        
        String refreshTokenStr = cookieRefreshToken != null ? cookieRefreshToken : (requestBody != null ? requestBody.getRefreshToken() : null);
        LoginResponse response = authService.refreshToken(refreshTokenStr);

        ResponseCookie accessCookie = createCookie(TokenType.ACCESS.getCookieName(), response.getToken(), TokenType.ACCESS.getMaxAgeSeconds(), "/");
        ResponseCookie refreshCookie = createCookie(TokenType.REFRESH.getCookieName(), response.getRefreshToken(), TokenType.REFRESH.getMaxAgeSeconds(), "/api/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(response, "Gia hạn phiên làm việc thành công!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "pmh_jwt_token", required = false) String accessCookieToken,
            @CookieValue(name = "pmh_refresh_token", required = false) String refreshCookieToken,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        String accessToken = resolveToken(accessCookieToken, authHeader);
        authService.logout(accessToken, refreshCookieToken);

        ResponseCookie clearAccessCookie = createCookie(TokenType.ACCESS.getCookieName(), "", 0, "/");
        ResponseCookie clearRefreshCookie = createCookie(TokenType.REFRESH.getCookieName(), "", 0, "/api/auth");

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
