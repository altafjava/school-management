package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateCertificateTemplateRequest;
import com.altafjava.school.api.dto.request.UpdateCertificateTemplateRequest;
import com.altafjava.school.api.dto.response.CertificateTemplateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Certificate Template", description = "APIs for managing Certificate Template operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface CertificateTemplateApi {

	@Operation(summary = "List", operationId = "certificatetemplate_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<CertificateTemplateResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "List active", operationId = "certificatetemplate_listActive")
	public ApiResponse<List<CertificateTemplateResponse>> listActive();

	@Operation(summary = "Get", operationId = "certificatetemplate_get")
	public ApiResponse<CertificateTemplateResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", operationId = "certificatetemplate_create")
	public ApiResponse<CertificateTemplateResponse> create(
			@Valid @RequestBody CreateCertificateTemplateRequest request);

	@Operation(summary = "Update details", operationId = "certificatetemplate_updateDetails")
	public ApiResponse<CertificateTemplateResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateCertificateTemplateRequest request);

	@Operation(summary = "Activate", operationId = "certificatetemplate_activate")
	public ApiResponse<CertificateTemplateResponse> activate(@PathVariable String publicId);

	@Operation(summary = "Deactivate", operationId = "certificatetemplate_deactivate")
	public ApiResponse<CertificateTemplateResponse> deactivate(@PathVariable String publicId);
}
