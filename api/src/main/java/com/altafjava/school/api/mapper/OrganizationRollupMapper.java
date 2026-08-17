package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AttendanceRollupResponse;
import com.altafjava.school.api.dto.response.CampusRollupResponse;
import com.altafjava.school.api.dto.response.FeeRollupResponse;
import com.altafjava.school.api.dto.response.OrganizationRollupResponse;
import com.altafjava.school.api.dto.response.RollupTotalsResponse;
import com.altafjava.school.domain.rollup.model.AttendanceRollup;
import com.altafjava.school.domain.rollup.model.CampusRollup;
import com.altafjava.school.domain.rollup.model.FeeRollup;
import com.altafjava.school.domain.rollup.model.OrganizationRollupReport;
import com.altafjava.school.domain.rollup.model.RollupTotals;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrganizationRollupMapper {

	@Mapping(target = "organizationPublicId", expression = "java(report.organizationPublicId().toString())")
	OrganizationRollupResponse toResponse(OrganizationRollupReport report);

	@Mapping(target = "tenantPublicId", expression = "java(campusRollup.tenantPublicId().toString())")
	CampusRollupResponse toResponse(CampusRollup campusRollup);

	RollupTotalsResponse toResponse(RollupTotals totals);

	@Mapping(target = "total", expression = "java(attendanceRollup.total())")
	AttendanceRollupResponse toResponse(AttendanceRollup attendanceRollup);

	FeeRollupResponse toResponse(FeeRollup feeRollup);
}
