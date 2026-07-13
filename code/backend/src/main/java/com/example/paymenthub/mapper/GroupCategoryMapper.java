package com.example.paymenthub.mapper;

import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.entity.GroupCategory;
import org.springframework.stereotype.Component;

@Component
public class GroupCategoryMapper {

    public GroupCategoryDTO toDTO(GroupCategory entity) {
        if (entity == null) {
            return null;
        }

        return GroupCategoryDTO.builder()
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
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .updatedBy(entity.getUpdatedBy())
                .updatedDate(entity.getUpdatedDate())
                .build();
    }

    public GroupCategory toEntity(GroupCategoryDTO dto) {
        if (dto == null) {
            return null;
        }

        return GroupCategory.builder()
                .id(dto.getId())
                .paramName(dto.getParamName())
                .paramValue(dto.getParamValue())
                .paramType(dto.getParamType())
                .description(dto.getDescription())
                .componentCode(dto.getComponentCode())
                .status(dto.getStatus())
                .isActive(dto.getIsActive())
                .isDisplay(dto.getIsDisplay())
                .newData(dto.getNewData())
                .effectiveDate(dto.getEffectiveDate())
                .endEffectiveDate(dto.getEndEffectiveDate())
                .build();
    }
}
