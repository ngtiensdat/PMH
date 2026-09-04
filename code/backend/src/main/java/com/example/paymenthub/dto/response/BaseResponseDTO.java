package com.example.paymenthub.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Base DTO chứa các audit fields dùng chung cho mọi ResponseDTO.
 * Subclass chỉ khai báo các field nghiệp vụ riêng của module.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class BaseResponseDTO {
    private Integer status;
    private Integer isActive;
    private Integer isDisplay;
    private String  newData;
    private LocalDateTime effectiveDate;
    private LocalDateTime endEffectiveDate;
    private Long    version;
    private String  createdBy;
    private LocalDateTime createdDate;
    private String  updatedBy;
    private LocalDateTime updatedDate;
}
