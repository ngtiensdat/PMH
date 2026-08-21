package com.example.paymenthub.dto.response;

import com.example.paymenthub.entity.ProcessingComponent;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentResponseDTO {
    private String componentCode;
    private String componentName;
    private String messageType;
    private String connectionMethod;
    private String checkToken;
    private String description;
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

    public static ComponentResponseDTO fromEntity(ProcessingComponent entity) {
        if (entity == null) return null;
        return ComponentResponseDTO.builder()
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
                .version(entity.getVersion())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .updatedBy(entity.getUpdatedBy())
                .updatedDate(entity.getUpdatedDate())
                .build();
    }
}
