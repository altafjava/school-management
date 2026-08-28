package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.request.CustomFieldVisibilityConditionRequest;
import com.altafjava.school.api.dto.response.CustomFieldVisibilityConditionResponse;
import com.altafjava.school.domain.customfield.model.CustomFieldVisibilityCondition;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CustomFieldVisibilityConditionMapper {

	@Mapping(target = "expectedValue", expression = "java(request.expectedValues() == null ? null : String.join(\",\", request.expectedValues()))")
	CustomFieldVisibilityCondition toDomain(CustomFieldVisibilityConditionRequest request);

	@Mapping(target = "expectedValues", expression = "java(condition.valueList())")
	CustomFieldVisibilityConditionResponse toResponse(CustomFieldVisibilityCondition condition);
}
