package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.LeaveBalanceResponse;
import com.altafjava.school.domain.leave.model.LeaveBalance;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LeaveBalanceMapper {

	@Mapping(target = "publicId", expression = "java(leaveBalance.getPublicId().toString())")
	@Mapping(target = "remainingDays", expression = "java(leaveBalance.remainingDays())")
	LeaveBalanceResponse toResponse(LeaveBalance leaveBalance);
}
