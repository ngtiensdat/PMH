package com.example.paymenthub.dto.response;

import com.example.paymenthub.entity.GroupCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class GroupCategoryResponseDTO extends BaseResponseDTO {
    private Long   id;
    private String paramName;
    private String paramValue;
    private String paramType;
    private String description;
    private String componentCode;
    private String componentName; // Từ JOIN (nếu có)

    public static GroupCategoryResponseDTO fromEntity(GroupCategory entity) {
        if (entity == null) return null;
        return GroupCategoryResponseDTO.builder()
                .id(entity.getId())
                .paramName(entity.getParamName())
                .paramValue(entity.getParamValue())
                .paramType(entity.getParamType())
                .description(entity.getDescription())
                .componentCode(entity.getComponentCode())
                // base fields
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
