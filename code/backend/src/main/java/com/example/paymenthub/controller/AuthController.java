package com.example.paymenthub.controller;

import com.example.paymenthub.common.base.ApiResponse;
import com.example.paymenthub.common.base.BaseController;
import com.example.paymenthub.dto.request.LoginRequest;
import com.example.paymenthub.dto.response.LoginResponse;
import com.example.paymenthub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.example.paymenthub.security.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private static final String COOKIE_NAME = "pmh_jwt_token";
    private static final long TOKEN_MAX_AGE_SECONDS = 86400L; // 24 giờ

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        ResponseCookie cookie = createJwtCookie(response.getToken(), TOKEN_MAX_AGE_SECONDS);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(response, "Đăng nhập thành công!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = COOKIE_NAME, required = false) String cookieToken,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        String token = resolveToken(cookieToken, authHeader);
        if (token != null) {
            authService.logout(token);
        }

        ResponseCookie cookie = createJwtCookie("", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(null, "Đăng xuất thành công!"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        LoginResponse response = authService.getCurrentUser(username);
        return ok(response, "Lấy thông tin tài khoản thành công!");
    }

    // ─── Private Helper Methods ──────────────────────────────────────────────

    /**
     * Tạo HttpOnly Cookie bọc JWT Token dùng chung cho Login và Logout
     */
    private ResponseCookie createJwtCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(COOKIE_NAME, value != null ? value : "")
                .httpOnly(true)
                .secure(false) // Đặt true khi chạy HTTPS trên môi trường Production
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
    }

    /**
     * Đọc Token từ Cookie hoặc Authorization Header
     */
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
