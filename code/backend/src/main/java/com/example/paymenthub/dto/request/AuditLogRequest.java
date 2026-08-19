package com.example.paymenthub.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequest {
    private String module;
    private String recordId;
    private String action;
    private String performedBy;
    private String oldData;
    private String newData;
    private String description;
    private Integer statusBefore;
    private Integer statusAfter;
}
