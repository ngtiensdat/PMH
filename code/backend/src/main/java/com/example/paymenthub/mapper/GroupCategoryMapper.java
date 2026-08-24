package com.example.paymenthub.mapper;

import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.entity.GroupCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GroupCategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "newData", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", expression = "java(com.example.paymenthub.common.enums.ParamStatus.NEW.getCode())")
    @Mapping(target = "isDisplay", expression = "java(com.example.paymenthub.common.enums.DisplayStatus.INITIAL.getCode())")
    @Mapping(target = "isActive", expression = "java(com.example.paymenthub.service.impl.GroupCategoryServiceImpl.computeActiveStatus(dto.getEffectiveDate(), dto.getEndEffectiveDate()))")
    @Mapping(target = "createdBy", source = "username")
    @Mapping(target = "updatedBy", source = "username")
    GroupCategory toEntity(GroupCategoryDTO dto, String username);
}
