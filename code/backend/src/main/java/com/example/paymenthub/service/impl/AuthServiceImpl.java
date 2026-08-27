package com.example.paymenthub.service.impl;

import com.example.paymenthub.common.enums.AuthErrorCode;
import com.example.paymenthub.common.exception.UnauthorizedAccessException;
import com.example.paymenthub.dto.request.LoginRequest;
import com.example.paymenthub.dto.response.LoginResponse;
import com.example.paymenthub.entity.Permission;
import com.example.paymenthub.entity.RefreshToken;
import com.example.paymenthub.entity.User;
import com.example.paymenthub.repository.RefreshTokenRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lockout-minutes}")
    private int lockoutMinutes;

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
            passwordEncoder.matches(password, DUMMY_PASSWORD_HASH);
            log.warn("[AuthService] Đăng nhập thất bại (User không tồn tại): username={}", username);
            throw new UnauthorizedAccessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getLockoutUntil() != null && !LocalDateTime.now().isBefore(user.getLockoutUntil())) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
        }

        if (user.isLocked()) {
            log.warn("[AuthService] Đăng nhập thất bại (Tài khoản đang bị khóa): username={}", username);
            throw new UnauthorizedAccessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

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

        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
        }

        List<String> roleCodes = user.getRoles() != null && !user.getRoles().isEmpty()
                ? user.getRoles().stream().map(r -> r.getRoleCode().replace("ROLE_", "")).distinct().toList()
                : List.of(user.getRole());

        List<String> permissionCodes = extractPermissions(user);

        String accessToken = jwtProvider.generateAccessToken(user.getUsername(), user.getRole(), permissionCodes);
        String refreshTokenStr = jwtProvider.generateRefreshToken(user.getUsername());

        refreshTokenRepository.revokeAllByUsername(user.getUsername());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .username(user.getUsername())
                .tokenHash(refreshTokenStr)
                .expiryDate(LocalDateTime.now().plusSeconds(com.example.paymenthub.common.enums.TokenType.REFRESH.getMaxAgeSeconds()))
                .revoked(false)
                .createdDate(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("[AuthService] Đăng nhập thành công: username={}, role={}, roles={}, permissions={}", 
                user.getUsername(), user.getRole(), roleCodes, permissionCodes);

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshTokenStr)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .roles(roleCodes)
                .permissions(permissionCodes)
                .build();
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshTokenStr) {
        if (refreshTokenStr == null || refreshTokenStr.trim().isEmpty() || !jwtProvider.validateToken(refreshTokenStr)) {
            throw new UnauthorizedAccessException(AuthErrorCode.INVALID_SESSION);
        }

        String tokenType = jwtProvider.getTypeFromToken(refreshTokenStr);
        if (!com.example.paymenthub.common.enums.TokenType.REFRESH.getTypeName().equals(tokenType)) {
            throw new UnauthorizedAccessException(AuthErrorCode.INVALID_SESSION);
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(refreshTokenStr)
                .orElseThrow(() -> new UnauthorizedAccessException(AuthErrorCode.INVALID_SESSION));

        if (storedToken.isRevoked() || storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedAccessException(AuthErrorCode.INVALID_SESSION);
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findByUsernameIgnoreCase(storedToken.getUsername())
                .orElseThrow(() -> new UnauthorizedAccessException(AuthErrorCode.USER_NOT_FOUND));

        if (user.isLocked()) {
            throw new UnauthorizedAccessException(AuthErrorCode.ACCOUNT_LOCKED);
        }

        List<String> permissionCodes = extractPermissions(user);
        String newAccessToken = jwtProvider.generateAccessToken(user.getUsername(), user.getRole(), permissionCodes);
        String newRefreshTokenStr = jwtProvider.generateRefreshToken(user.getUsername());

        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .username(user.getUsername())
                .tokenHash(newRefreshTokenStr)
                .expiryDate(LocalDateTime.now().plusSeconds(com.example.paymenthub.common.enums.TokenType.REFRESH.getMaxAgeSeconds()))
                .revoked(false)
                .createdDate(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(newRefreshTokenEntity);

        List<String> roleCodes = user.getRoles() != null && !user.getRoles().isEmpty()
                ? user.getRoles().stream().map(r -> r.getRoleCode().replace("ROLE_", "")).distinct().toList()
                : List.of(user.getRole());

        return LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshTokenStr)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .roles(roleCodes)
                .permissions(permissionCodes)
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

        List<String> roleCodes = user.getRoles() != null && !user.getRoles().isEmpty()
                ? user.getRoles().stream().map(r -> r.getRoleCode().replace("ROLE_", "")).distinct().toList()
                : List.of(user.getRole());

        List<String> permissionCodes = extractPermissions(user);

        return LoginResponse.builder()
                .token(null)
                .refreshToken(null)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .roles(roleCodes)
                .permissions(permissionCodes)
                .build();
    }

    @Override
    @Transactional
    public void logout(String token) {
        if (token != null && !token.trim().isEmpty()) {
            java.util.Date expiry = jwtProvider.getExpirationFromToken(token);
            long expiryTime = expiry != null ? expiry.getTime() : System.currentTimeMillis() + com.example.paymenthub.common.enums.TokenType.ACCESS.getExpirationMs();
            tokenBlacklistService.blacklistToken(token, expiryTime);
            log.info("[AuthService] Đã thu hồi Access Token và đưa vào Blacklist khi Đăng xuất.");
        }
    }

    @Override
    @Transactional
    public void logout(String accessTokenStr, String refreshTokenStr) {
        if (accessTokenStr != null && !accessTokenStr.trim().isEmpty()) {
            logout(accessTokenStr);
        }
        if (refreshTokenStr != null && !refreshTokenStr.trim().isEmpty()) {
            refreshTokenRepository.findByTokenHash(refreshTokenStr).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }
    }

    private List<String> extractPermissions(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }
        return user.getRoles().stream()
                .filter(r -> r.getPermissions() != null)
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getPermissionCode)
                .distinct()
                .toList();
    }
}
