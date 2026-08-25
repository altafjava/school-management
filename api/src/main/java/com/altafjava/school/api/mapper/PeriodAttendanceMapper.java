package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.PeriodAttendanceResponse;
import com.altafjava.school.domain.attendance.model.PeriodAttendance;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PeriodAttendanceMapper {

	@Mapping(target = "publicId", expression = "java(periodAttendance.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(periodAttendance.getStatus().name())")
	PeriodAttendanceResponse toResponse(PeriodAttendance periodAttendance);
}
