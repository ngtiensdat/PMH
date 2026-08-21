package com.example.paymenthub.dto.response;

import com.example.paymenthub.entity.GroupCategory;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupCategoryResponseDTO {
    private Long id;
    private String paramName;
    private String paramValue;
    private String paramType;
    private String description;
    private String componentCode;
    private Integer status;
    private Integer isActive;
    private Integer isDisplay;
    private String newData;
    private LocalDateTime effectiveDate;
    private LocalDateTime endEffectiveDate;
    private Long version;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;

    public static GroupCategoryResponseDTO fromEntity(GroupCategory entity) {
        if (entity == null) return null;
        return GroupCategoryResponseDTO.builder()
                .id(entity.getId())
                .paramName(entity.getParamName())
                .paramValue(entity.getParamValue())
                .paramType(entity.getParamType())
                .description(entity.getDescription())
                .componentCode(entity.getComponentCode())
                .status(entity.getStatus())
                .isActive(entity.getIsActive())
                .isDisplay(entity.getIsDisplay())
                .newData(entity.getNewData())
                .effectiveDate(entity.getEffectiveDate())
                .endEffectiveDate(entity.getEndEffectiveDate())
                .version(entity.getVersion())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .updatedBy(entity.getUpdatedBy())
                .updatedDate(entity.getUpdatedDate())
                .build();
    }
}
