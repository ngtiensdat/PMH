package com.example.paymenthub.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Các loại hành động được ghi trong Lịch sử thao tác (Audit Log).
 */
@Getter
@RequiredArgsConstructor
public enum AuditAction {

    CREATE("Tạo mới"),
    UPDATE("Cập nhật"),
    DELETE("Xóa"),
    SEND_APPROVAL("Gửi duyệt"),
    APPROVE("Phê duyệt"),
    REJECT("Từ chối"),
    CANCEL_APPROVAL("Hủy duyệt");

    private final String actionName;
}
