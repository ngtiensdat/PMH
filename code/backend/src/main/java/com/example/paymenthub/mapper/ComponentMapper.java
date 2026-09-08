package com.example.paymenthub.mapper;

import com.example.paymenthub.dto.request.ComponentDTO;
import com.example.paymenthub.entity.ProcessingComponent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ComponentMapper {

    @Mapping(target = "componentCode", source = "dto.componentCode", qualifiedByName = "toUpperCase")
    @Mapping(target = "checkToken", expression = "java(dto.getCheckToken() != null ? dto.getCheckToken() : \"N\")")
    @Mapping(target = "newData", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "status", expression = "java(com.example.paymenthub.common.enums.ParamStatus.NEW.getCode())")
    @Mapping(target = "isDisplay", expression = "java(com.example.paymenthub.common.enums.DisplayStatus.INITIAL.getCode())")
    @Mapping(target = "isActive", expression = "java(com.example.paymenthub.common.util.DateUtils.computeActiveStatus(dto.getEffectiveDate(), dto.getEndEffectiveDate()))")
    @Mapping(target = "createdBy", source = "username")
    @Mapping(target = "updatedBy", source = "username")
    ProcessingComponent toEntity(ComponentDTO dto, String username);

    @Named("toUpperCase")
    default String toUpperCase(String value) {
        return value != null ? value.toUpperCase() : null;
    }
}
