package com.example.paymenthub.mapper;

import com.example.paymenthub.dto.request.ComponentDTO;
import com.example.paymenthub.entity.ProcessingComponent;
import org.springframework.stereotype.Service;

@Service
public class ComponentMapper {

    public ComponentDTO toDTO(ProcessingComponent entity) {
        if (entity == null) return null;

        return ComponentDTO.builder()
                .componentCode(entity.getComponentCode())
                .componentName(entity.getComponentName())
                .messageType(entity.getMessageType())
                .connectionMethod(entity.getConnectionMethod())
                .checkToken(entity.getCheckToken())
                .description(entity.getDescription())
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

    public ProcessingComponent toEntity(ComponentDTO dto) {
        if (dto == null) return null;

        return ProcessingComponent.builder()
                .componentCode(dto.getComponentCode())
                .componentName(dto.getComponentName())
                .messageType(dto.getMessageType())
                .connectionMethod(dto.getConnectionMethod())
                .checkToken(dto.getCheckToken())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .isActive(dto.getIsActive())
                .isDisplay(dto.getIsDisplay())
                .newData(dto.getNewData())
                .effectiveDate(dto.getEffectiveDate())
                .endEffectiveDate(dto.getEndEffectiveDate())
                .build();
    }
}
