package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.MarkAttendanceRequest;
import com.altafjava.school.api.dto.request.UpdateAttendanceStatusRequest;
import com.altafjava.school.api.dto.response.AttendanceCorrectionResponse;
import com.altafjava.school.api.dto.response.AttendanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Attendance", description = "APIs for managing Attendance operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AttendanceApi {

	@Operation(summary = "List", operationId = "attendance_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<AttendanceResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "attendance_get")
	public ApiResponse<AttendanceResponse> get(@PathVariable String publicId);

	@Operation(summary = "Mark", operationId = "attendance_mark")
	public ApiResponse<AttendanceResponse> mark(@Valid @RequestBody MarkAttendanceRequest request);

	@Operation(summary = "Update status", operationId = "attendance_updateStatus")
	public ApiResponse<AttendanceResponse> updateStatus(@PathVariable String publicId,
			@Valid @RequestBody UpdateAttendanceStatusRequest request);

	@Operation(summary = "List corrections", operationId = "attendance_listCorrections")
	public ApiResponse<com.altafjava.platform.core.model.Page<AttendanceCorrectionResponse>> listCorrections(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Delete", operationId = "attendance_delete")
	public ApiResponse<Void> delete(@PathVariable String publicId);
}
