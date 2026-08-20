package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AttendancePercentageResponse;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AttendancePercentageMapper {

	AttendancePercentageResponse toResponse(AttendancePercentage attendancePercentage);
}
