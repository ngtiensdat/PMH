package com.example.paymenthub.mapper;

import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.entity.GroupCategory;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-04T14:02:10+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.100.v20260826-1225, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class GroupCategoryMapperImpl implements GroupCategoryMapper {

    @Override
    public GroupCategory toEntity(GroupCategoryDTO dto, String username) {
        if ( dto == null && username == null ) {
            return null;
        }

        GroupCategory.GroupCategoryBuilder<?, ?> groupCategory = GroupCategory.builder();

        if ( dto != null ) {
            groupCategory.effectiveDate( dto.getEffectiveDate() );
            groupCategory.endEffectiveDate( dto.getEndEffectiveDate() );
            groupCategory.componentCode( dto.getComponentCode() );
            groupCategory.description( dto.getDescription() );
            groupCategory.paramName( dto.getParamName() );
            groupCategory.paramType( dto.getParamType() );
            groupCategory.paramValue( dto.getParamValue() );
        }
        if ( username != null ) {
            groupCategory.createdBy( username );
            groupCategory.updatedBy( username );
        }
        groupCategory.status( com.example.paymenthub.common.enums.ParamStatus.NEW.getCode() );
        groupCategory.isDisplay( com.example.paymenthub.common.enums.DisplayStatus.INITIAL.getCode() );
        groupCategory.isActive( com.example.paymenthub.common.util.DateUtils.computeActiveStatus(dto.getEffectiveDate(), dto.getEndEffectiveDate()) );

        return groupCategory.build();
    }
}
