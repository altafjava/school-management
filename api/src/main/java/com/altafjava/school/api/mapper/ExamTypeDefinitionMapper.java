package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.ExamTypeDefinitionResponse;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ExamTypeDefinitionMapper {

	@Mapping(target = "publicId", expression = "java(definition.getPublicId().toString())")
	ExamTypeDefinitionResponse toResponse(ExamTypeDefinition definition);
}
