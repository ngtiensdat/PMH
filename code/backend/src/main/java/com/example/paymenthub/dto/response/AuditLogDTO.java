package com.example.paymenthub.dto.response;

import java.time.LocalDateTime;

public record AuditLogDTO(
        Long id,
        String module,
        String recordId,
        String action,
        String performedBy,
        LocalDateTime actionDate,
        String description,
        String oldData,
        String newDataLog,
        Integer statusBefore,
        Integer statusAfter
) {}
