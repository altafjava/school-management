package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.SubmissionResponse;
import com.altafjava.school.domain.lms.model.Submission;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubmissionMapper {

	@Mapping(target = "publicId", expression = "java(submission.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(submission.getStatus().name())")
	SubmissionResponse toResponse(Submission submission);
}
