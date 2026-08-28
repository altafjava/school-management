package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.request.CustomFieldValidationRuleRequest;
import com.altafjava.school.api.dto.response.CustomFieldValidationRuleResponse;
import com.altafjava.school.domain.customfield.model.CustomFieldValidationRule;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CustomFieldValidationRuleMapper {

	@Mapping(target = "options", expression = "java(request.options() == null ? null : String.join(\",\", request.options()))")
	CustomFieldValidationRule toDomain(CustomFieldValidationRuleRequest request);

	@Mapping(target = "options", expression = "java(rule.optionList())")
	CustomFieldValidationRuleResponse toResponse(CustomFieldValidationRule rule);
}
