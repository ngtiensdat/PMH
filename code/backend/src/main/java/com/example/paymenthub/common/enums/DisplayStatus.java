package com.example.paymenthub.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái hiển thị đối chiếu (1: Chưa từng duyệt, 2: Đã từng duyệt).
 */
@Getter
@RequiredArgsConstructor
public enum DisplayStatus {

    INITIAL(1, "Chưa từng duyệt"),
    ONCE_APPROVED(2, "Đã từng duyệt");

    private final int code;
    private final String description;

    public static DisplayStatus fromCode(Integer code) {
        if (code == null) return null;
        for (DisplayStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Mã trạng thái hiển thị không hợp lệ: " + code);
    }
}
