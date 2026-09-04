package com.example.paymenthub.dto.response;

import com.example.paymenthub.entity.ProcessingComponent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ComponentResponseDTO extends BaseResponseDTO {
    private String componentCode;
    private String componentName;
    private String messageType;
    private String connectionMethod;
    private String checkToken;
    private String description;

    public static ComponentResponseDTO fromEntity(ProcessingComponent entity) {
        if (entity == null) return null;
        return ComponentResponseDTO.builder()
                .componentCode(entity.getComponentCode())
                .componentName(entity.getComponentName())
                .messageType(entity.getMessageType())
                .connectionMethod(entity.getConnectionMethod())
                .checkToken(entity.getCheckToken())
                .description(entity.getDescription())
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
