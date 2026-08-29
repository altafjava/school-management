package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.MarkPeriodAttendanceRequest;
import com.altafjava.school.api.dto.response.PeriodAttendanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Period Attendance", description = "APIs for managing Period Attendance operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface PeriodAttendanceApi {

	@Operation(summary = "List", operationId = "periodattendance_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<PeriodAttendanceResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "periodattendance_get")
	public ApiResponse<PeriodAttendanceResponse> get(@PathVariable String publicId);

	@Operation(summary = "Mark", operationId = "periodattendance_mark")
	public ApiResponse<PeriodAttendanceResponse> mark(@Valid @RequestBody MarkPeriodAttendanceRequest request);
}
