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

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        // Tạo HttpOnly Cookie chứa JWT Token (SameSite=Lax cho phép gửi Cookie giữa localhost:4200 và localhost:8080)
        ResponseCookie cookie = ResponseCookie.from("pmh_jwt_token", response.getToken())
                .httpOnly(true)
                .secure(false) // Đặt true khi chạy HTTPS trên môi trường Production
                .path("/")
                .maxAge(86400) // 24 giờ
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(response, "Đăng nhập thành công!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "pmh_jwt_token", required = false) String cookieToken,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        String token = null;
        if (cookieToken != null && !cookieToken.trim().isEmpty()) {
            token = cookieToken;
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token != null) {
            authService.logout(token);
        }

        // Xóa Cookie bằng cách đặt Max-Age = 0
        ResponseCookie cookie = ResponseCookie.from("pmh_jwt_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

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
}
