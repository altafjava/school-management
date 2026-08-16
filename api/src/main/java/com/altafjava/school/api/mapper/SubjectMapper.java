package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.SubjectResponse;
import com.altafjava.school.domain.subject.model.Subject;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubjectMapper {

	@Mapping(target = "publicId", expression = "java(subject.getPublicId().toString())")
	SubjectResponse toResponse(Subject subject);
}
