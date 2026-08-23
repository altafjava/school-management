package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CurriculumResponse;
import com.altafjava.school.domain.curriculum.model.Curriculum;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CurriculumMapper {

	@Mapping(target = "publicId", expression = "java(curriculum.getPublicId().toString())")
	CurriculumResponse toResponse(Curriculum curriculum);
}
