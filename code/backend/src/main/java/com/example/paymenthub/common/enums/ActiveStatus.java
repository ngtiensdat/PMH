package com.example.paymenthub.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tình trạng hoạt động của tham số phục vụ hệ thống Payment Hub.
 */
@Getter
@RequiredArgsConstructor
public enum ActiveStatus {

    INACTIVE(0, "Không hoạt động"),
    ACTIVE(1, "Đang hoạt động");

    private final int code;
    private final String description;

    public static ActiveStatus fromCode(Integer code) {
        if (code == null) return null;
        for (ActiveStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Mã tình trạng hoạt động không hợp lệ: " + code);
    }
}
