package com.example.paymenthub.controller;

import com.example.paymenthub.common.base.ApiResponse;
import com.example.paymenthub.common.base.BaseController;
import com.example.paymenthub.dto.response.AuditLogDTO;
import com.example.paymenthub.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
public class AuditLogController extends BaseController {

    private final AuditLogService auditLogService;

    /**
     * Lấy lịch sử thao tác cho một Tham số danh mục theo nhóm.
     * GET /api/audit-log/group-category/{id}
     */
    @GetMapping("/group-category/{id}")
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> getGroupCategoryHistory(
            @PathVariable Long id,
            @PageableDefault(page = 0, size = 5) Pageable pageable
    ) {
        Page<AuditLogDTO> history = auditLogService.getHistory("GROUP_CATEGORY", String.valueOf(id), pageable);
        return ok(history, "Lấy lịch sử thao tác thành công");
    }

    /**
     * Lấy lịch sử thao tác cho một Cấu phần xử lý.
     * GET /api/audit-log/component/{code}
     */
    @GetMapping("/component/{code}")
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> getComponentHistory(
            @PathVariable String code,
            @PageableDefault(page = 0, size = 5) Pageable pageable
    ) {
        Page<AuditLogDTO> history = auditLogService.getHistory("COMPONENT", code, pageable);
        return ok(history, "Lấy lịch sử thao tác thành công");
    }
}
