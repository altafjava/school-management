package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AttendanceCorrectionResponse;
import com.altafjava.school.domain.attendance.model.AttendanceCorrection;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AttendanceCorrectionMapper {

	@Mapping(target = "publicId", expression = "java(correction.getPublicId().toString())")
	@Mapping(target = "correctedBy", source = "createdBy")
	@Mapping(target = "correctedAt", source = "createdAt")
	AttendanceCorrectionResponse toResponse(AttendanceCorrection correction);
}
