package com.example.paymenthub.service.impl;

import com.example.paymenthub.common.enums.AuthErrorCode;
import com.example.paymenthub.common.exception.UnauthorizedAccessException;
import com.example.paymenthub.dto.request.LoginRequest;
import com.example.paymenthub.dto.response.LoginResponse;
import com.example.paymenthub.entity.User;
import com.example.paymenthub.repository.UserRepository;
import com.example.paymenthub.security.JwtProvider;
import com.example.paymenthub.security.TokenBlacklistService;
import com.example.paymenthub.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lockout-minutes}")
    private int lockoutMinutes;

    // Dummy BCrypt hash sử dụng để cân bằng thời gian xử lý (chống Timing Attack khi user không tồn tại)
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07Xd0LDMxs.55k.4pS";

    @Override
    @Transactional(noRollbackFor = UnauthorizedAccessException.class)
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            throw new UnauthorizedAccessException(AuthErrorCode.CREDENTIALS_REQUIRED);
        }

        String username = request.getUsername().trim();
        String password = request.getPassword();

        User user = userRepository.findByUsernameIgnoreCase(username).orElse(null);

        if (user == null) {
            // Ngăn ngừa Timing Attack: Chạy mã hóa password giả lập như khi user tồn tại
            passwordEncoder.matches(password, DUMMY_PASSWORD_HASH);
            log.warn("[AuthService] Đăng nhập thất bại (User không tồn tại): username={}", username);
            throw new UnauthorizedAccessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // Nếu thời gian khóa đã hết hạn, tự động reset lại bộ đếm lần sai
        if (user.getLockoutUntil() != null && !LocalDateTime.now().isBefore(user.getLockoutUntil())) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
        }

        // Kiểm tra tài khoản có đang bị khóa hay không
        if (user.isLocked()) {
            log.warn("[AuthService] Đăng nhập thất bại (Tài khoản đang bị khóa): username={}", username);
            // Trả về INVALID_CREDENTIALS chung để chống User Enumeration Attack (dò tên tài khoản qua phản hồi khóa)
            throw new UnauthorizedAccessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // Kiểm tra mật khẩu
        boolean matches = passwordEncoder.matches(password, user.getPasswordHash());

        if (!matches) {
            int attempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() + 1 : 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= maxFailedAttempts) {
                user.setLockoutUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));
                log.warn("[AuthService] Tài khoản {} bị khóa {} phút do nhập sai mật khẩu {} lần (cấu hình max={})", 
                        username, lockoutMinutes, attempts, maxFailedAttempts);
            } else {
                log.warn("[AuthService] Đăng nhập thất bại cho username={}. Số lần sai: {}/{} (cấu hình max={})", 
                        username, attempts, maxFailedAttempts, maxFailedAttempts);
            }

            userRepository.save(user);
            throw new UnauthorizedAccessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // Reset số lần đăng nhập sai khi đăng nhập thành công
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
        }

        String token = jwtProvider.generateToken(user.getUsername(), user.getRole());
        log.info("[AuthService] Đăng nhập thành công: username={}, role={}", user.getUsername(), user.getRole());

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse getCurrentUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new UnauthorizedAccessException(AuthErrorCode.INVALID_SESSION);
        }

        User user = userRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new UnauthorizedAccessException(AuthErrorCode.USER_NOT_FOUND));

        if (user.isLocked()) {
            throw new UnauthorizedAccessException(AuthErrorCode.ACCOUNT_LOCKED);
        }

        return LoginResponse.builder()
                .token(null)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.trim().isEmpty()) {
            java.util.Date expiry = jwtProvider.getExpirationFromToken(token);
            long expiryTime = expiry != null ? expiry.getTime() : System.currentTimeMillis() + 86400000L;
            tokenBlacklistService.blacklistToken(token, expiryTime);
            log.info("[AuthService] Đã thu hồi Token và đưa vào Blacklist khi Đăng xuất.");
        }
    }
}
