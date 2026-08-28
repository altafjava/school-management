package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.GradeCorrectionResponse;
import com.altafjava.school.domain.grade.model.GradeCorrection;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GradeCorrectionMapper {

	@Mapping(target = "publicId", expression = "java(correction.getPublicId().toString())")
	@Mapping(target = "correctedBy", source = "createdBy")
	@Mapping(target = "correctedAt", source = "createdAt")
	GradeCorrectionResponse toResponse(GradeCorrection correction);
}
