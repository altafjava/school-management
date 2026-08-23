package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.LeaveTypeResponse;
import com.altafjava.school.domain.leave.model.LeaveType;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LeaveTypeMapper {

	@Mapping(target = "publicId", expression = "java(leaveType.getPublicId().toString())")
	LeaveTypeResponse toResponse(LeaveType leaveType);
}
