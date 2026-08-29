package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AssignExamTermRequest;
import com.altafjava.school.api.dto.request.RescheduleExamRequest;
import com.altafjava.school.api.dto.request.ScheduleExamRequest;
import com.altafjava.school.api.dto.response.ExamResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Exam", description = "APIs for managing Exam operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface ExamApi {

	@Operation(summary = "List", operationId = "exam_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<ExamResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "exam_get")
	public ApiResponse<ExamResponse> get(@PathVariable String publicId);

	@Operation(summary = "Schedule", operationId = "exam_schedule", description = "Schedules a new exam against a tenant-defined exam type "
			+ "(see Exam Type Definition) — examTypeId must reference an existing, active catalog entry.")
	public ApiResponse<ExamResponse> schedule(@Valid @RequestBody ScheduleExamRequest request);

	@Operation(summary = "Reschedule", operationId = "exam_reschedule")
	public ApiResponse<ExamResponse> reschedule(@PathVariable String publicId,
			@Valid @RequestBody RescheduleExamRequest request);

	@Operation(summary = "Assign term", operationId = "exam_assignTerm")
	public ApiResponse<ExamResponse> assignTerm(@PathVariable String publicId,
			@Valid @RequestBody AssignExamTermRequest request);

	@Operation(summary = "Complete", operationId = "exam_complete", description = "Marks the exam complete, making it eligible for grade entry and downstream "
			+ "GPA/report-card computation.")
	public ApiResponse<ExamResponse> complete(@PathVariable String publicId);

	@Operation(summary = "Cancel", operationId = "exam_cancel")
	public ApiResponse<ExamResponse> cancel(@PathVariable String publicId);
}
