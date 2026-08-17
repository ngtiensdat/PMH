package com.example.paymenthub.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tên module trong hệ thống Payment Hub.
 */
@Getter
@RequiredArgsConstructor
public enum ModuleType {

    GROUP_CATEGORY("GROUP_CATEGORY", "Tham số danh mục theo nhóm"),
    COMPONENT("COMPONENT", "Tham số cấu phần xử lý");

    private final String code;
    private final String description;
}
