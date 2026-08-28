package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CustomFieldDefinitionResponse;
import com.altafjava.school.domain.customfield.model.CustomFieldDefinition;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = {
		CustomFieldValidationRuleMapper.class, CustomFieldVisibilityConditionMapper.class })
public interface CustomFieldDefinitionMapper {

	@Mapping(target = "publicId", expression = "java(customFieldDefinition.getPublicId().toString())")
	CustomFieldDefinitionResponse toResponse(CustomFieldDefinition customFieldDefinition);
}
