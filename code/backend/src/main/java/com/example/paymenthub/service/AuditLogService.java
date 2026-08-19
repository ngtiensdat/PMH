package com.example.paymenthub.service;

import com.example.paymenthub.dto.request.AuditLogRequest;
import com.example.paymenthub.dto.response.AuditLogDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    /**
     * Ghi nhật ký thao tác sử dụng DTO AuditLogRequest (Chuẩn tối ưu)
     */
    void log(AuditLogRequest request);

    /**
     * Backward-compatible default method cho các vị trí gọi truyền thống
     */
    default void log(String module, String recordId, String action, String performedBy,
                     String oldData, String newData, String description,
                     Integer statusBefore, Integer statusAfter) {
        log(AuditLogRequest.builder()
                .module(module)
                .recordId(recordId)
                .action(action)
                .performedBy(performedBy)
                .oldData(oldData)
                .newData(newData)
                .description(description)
                .statusBefore(statusBefore)
                .statusAfter(statusAfter)
                .build());
    }

    /**
     * Lấy lịch sử thao tác của một bản ghi.
     */
    Page<AuditLogDTO> getHistory(String module, String recordId, Pageable pageable);
}
