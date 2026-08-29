package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.PayComponentDefinitionResponse;
import com.altafjava.school.domain.payroll.model.PayComponentDefinition;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PayComponentDefinitionMapper {

	@Mapping(target = "publicId", expression = "java(definition.getPublicId().toString())")
	PayComponentDefinitionResponse toResponse(PayComponentDefinition definition);
}
