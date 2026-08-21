package com.example.paymenthub.service.impl;

import com.example.paymenthub.common.exception.UnauthorizedAccessException;
import com.example.paymenthub.dto.request.LoginRequest;
import com.example.paymenthub.dto.response.LoginResponse;
import com.example.paymenthub.security.JwtProvider;
import com.example.paymenthub.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    // Danh sách tài khoản kiểm thử mặc định (Mật khẩu được mã hóa BCrypt)
    private static final Map<String, UserCredentials> USERS = new HashMap<>();

    static {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        USERS.put("MAKE", new UserCredentials("make", encoder.encode("123"), "Maker User (make)", "MAKER"));
        USERS.put("CHECK", new UserCredentials("check", encoder.encode("123"), "Checker User (check)", "CHECKER"));
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim().toUpperCase();
        String password = request.getPassword();

        UserCredentials user = USERS.get(username);
        // Kiểm tra khớp mật khẩu sử dụng BCrypt PasswordEncoder
        if (user == null || !passwordEncoder.matches(password, user.passwordHash)) {
            log.warn("[AuthService] Đăng nhập thất bại cho username={}", username);
            throw new UnauthorizedAccessException("Tên đăng nhập hoặc mật khẩu không chính xác!");
        }

        String token = jwtProvider.generateToken(user.username, user.role);
        log.info("[AuthService] Đăng nhập thành công: username={}, role={}", user.username, user.role);

        return LoginResponse.builder()
                .token(token)
                .username(user.username)
                .fullName(user.fullName)
                .role(user.role)
                .build();
    }

    @Override
    public LoginResponse getCurrentUser(String username) {
        if (username == null) {
            throw new UnauthorizedAccessException("Phiên làm việc không hợp lệ!");
        }
        UserCredentials user = USERS.get(username.trim().toUpperCase());
        if (user == null) {
            throw new UnauthorizedAccessException("Người dùng không tồn tại!");
        }
        return LoginResponse.builder()
                .token(null)
                .username(user.username)
                .fullName(user.fullName)
                .role(user.role)
                .build();
    }

    private static class UserCredentials {
        String username;
        String passwordHash;
        String fullName;
        String role;

        UserCredentials(String username, String passwordHash, String fullName, String role) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.fullName = fullName;
            this.role = role;
        }
    }
}
