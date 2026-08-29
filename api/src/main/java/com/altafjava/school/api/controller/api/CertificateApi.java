package com.altafjava.school.api.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.response.CertificateIssuanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Certificate", description = "APIs for managing Certificate operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface CertificateApi {

	@Operation(summary = "List", operationId = "certificate_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<CertificateIssuanceResponse>> list(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Issue", operationId = "certificate_issue")
	public ApiResponse<CertificateIssuanceResponse> issue(@PathVariable String studentPublicId,
			@RequestParam String certificateTemplatePublicId,
			@AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Download", operationId = "certificate_download")
	public ResponseEntity<byte[]> download(@PathVariable String studentPublicId,
			@PathVariable String certificatePublicId);
}
