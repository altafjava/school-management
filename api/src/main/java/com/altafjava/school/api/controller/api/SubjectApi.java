package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AssignSubjectCurriculumRequest;
import com.altafjava.school.api.dto.request.CreateSubjectRequest;
import com.altafjava.school.api.dto.response.SubjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Subject", description = "APIs for managing Subject operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface SubjectApi {

	@Operation(summary = "List", operationId = "subject_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<SubjectResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "subject_get")
	public ApiResponse<SubjectResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", operationId = "subject_create")
	public ApiResponse<SubjectResponse> create(@Valid @RequestBody CreateSubjectRequest request);

	@Operation(summary = "Deactivate", operationId = "subject_deactivate")
	public ApiResponse<SubjectResponse> deactivate(@PathVariable String publicId);

	@Operation(summary = "Assign curriculum", operationId = "subject_assignCurriculum")
	public ApiResponse<SubjectResponse> assignCurriculum(@PathVariable String publicId,
			@Valid @RequestBody AssignSubjectCurriculumRequest request);
}
