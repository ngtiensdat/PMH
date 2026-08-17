package com.example.paymenthub.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái của bản ghi tham số trong quy trình phê duyệt Maker - Checker.
 */
@Getter
@RequiredArgsConstructor
public enum ParamStatus {

    NEW(1, "Tạo mới"),
    PENDING(3, "Chờ duyệt"),
    APPROVED(4, "Đã duyệt"),
    REJECTED(5, "Từ chối"),
    CANCELED(7, "Hủy duyệt");

    private final int code;
    private final String description;

    /**
     * Tìm kiếm Enum từ mã trạng thái Integer.
     */
    public static ParamStatus fromCode(Integer code) {
        if (code == null) return null;
        for (ParamStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Mã trạng thái không hợp lệ: " + code);
    }
}
