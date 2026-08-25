package com.example.paymenthub.mapper;

import com.example.paymenthub.dto.request.ComponentDTO;
import com.example.paymenthub.entity.ProcessingComponent;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-25T11:32:12+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class ComponentMapperImpl implements ComponentMapper {

    @Override
    public ProcessingComponent toEntity(ComponentDTO dto, String username) {
        if ( dto == null && username == null ) {
            return null;
        }

        ProcessingComponent.ProcessingComponentBuilder<?, ?> processingComponent = ProcessingComponent.builder();

        if ( dto != null ) {
            processingComponent.componentCode( toUpperCase( dto.getComponentCode() ) );
            processingComponent.createdDate( dto.getCreatedDate() );
            processingComponent.effectiveDate( dto.getEffectiveDate() );
            processingComponent.endEffectiveDate( dto.getEndEffectiveDate() );
            processingComponent.updatedDate( dto.getUpdatedDate() );
            processingComponent.componentName( dto.getComponentName() );
            processingComponent.connectionMethod( dto.getConnectionMethod() );
            processingComponent.description( dto.getDescription() );
            processingComponent.messageType( dto.getMessageType() );
        }
        if ( username != null ) {
            processingComponent.createdBy( username );
            processingComponent.updatedBy( username );
        }
        processingComponent.checkToken( dto.getCheckToken() != null ? dto.getCheckToken() : "N" );
        processingComponent.status( com.example.paymenthub.common.enums.ParamStatus.NEW.getCode() );
        processingComponent.isDisplay( com.example.paymenthub.common.enums.DisplayStatus.INITIAL.getCode() );
        processingComponent.isActive( com.example.paymenthub.service.impl.ComponentServiceImpl.computeActiveStatus(dto.getEffectiveDate(), dto.getEndEffectiveDate()) );

        return processingComponent.build();
    }
}
