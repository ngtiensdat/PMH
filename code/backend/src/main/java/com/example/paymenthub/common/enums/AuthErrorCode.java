package com.example.paymenthub.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Danh mục mã lỗi và thông điệp lỗi cho xác thực (Authentication & Authorization).
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {

    CREDENTIALS_REQUIRED("AUTH_001", "Tên đăng nhập hoặc mật khẩu không được để trống!"),
    INVALID_CREDENTIALS("AUTH_002", "Tên đăng nhập hoặc mật khẩu không chính xác!"),
    ACCOUNT_LOCKED("AUTH_003", "Tài khoản đã bị tạm khóa do nhập sai mật khẩu nhiều lần. Vui lòng thử lại sau!"),
    INVALID_SESSION("AUTH_004", "Phiên làm việc không hợp lệ!"),
    USER_NOT_FOUND("AUTH_005", "Người dùng không tồn tại!");

    private final String code;
    private final String message;
}
