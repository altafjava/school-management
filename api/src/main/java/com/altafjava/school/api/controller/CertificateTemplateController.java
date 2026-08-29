package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.CertificateTemplateApi;
import com.altafjava.school.api.dto.request.CreateCertificateTemplateRequest;
import com.altafjava.school.api.dto.request.UpdateCertificateTemplateRequest;
import com.altafjava.school.api.dto.response.CertificateTemplateResponse;
import com.altafjava.school.api.mapper.CertificateTemplateMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.CertificateTemplateService;

// Tenant-admin-defined certificate catalog (e.g. "Bonafide Certificate", "Transfer Certificate").
// No dedicated REGISTRAR role exists in this codebase, so "admin/registrar" access is granted to
// TENANT_ADMIN (full CRUD) and PRINCIPAL (read-only), the closest existing school-admin role,
// mirroring how PRINCIPAL is already used for other admin-adjacent read surfaces.
@RestController
@RequestMapping("/api/v1/certificate-templates")
public class CertificateTemplateController implements CertificateTemplateApi {

	private final CertificateTemplateService certificateTemplateService;
	private final CertificateTemplateMapper certificateTemplateMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CertificateTemplateController(CertificateTemplateService certificateTemplateService,
			CertificateTemplateMapper certificateTemplateMapper, SpringDataPageableResolver pageableResolver) {
		this.certificateTemplateService = certificateTemplateService;
		this.certificateTemplateMapper = certificateTemplateMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<CertificateTemplateResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(
				PlatformPageMapper.toPlatformPage(certificateTemplateService.list(pageableResolver.resolve(page, size))
						.map(certificateTemplateMapper::toResponse)));
	}

	@Override
	@GetMapping("/active")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_READ')")
	public ApiResponse<List<CertificateTemplateResponse>> listActive() {
		return ApiResponse.success(
				certificateTemplateService.listActive().stream().map(certificateTemplateMapper::toResponse).toList());
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_READ')")
	public ApiResponse<CertificateTemplateResponse> get(@PathVariable String publicId) {
		return ApiResponse
				.success(certificateTemplateMapper.toResponse(certificateTemplateService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_WRITE')")
	public ApiResponse<CertificateTemplateResponse> create(
			@Valid @RequestBody CreateCertificateTemplateRequest request) {
		return ApiResponse.success(certificateTemplateMapper
				.toResponse(certificateTemplateService.create(request.name(), request.bodyTemplate())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_WRITE')")
	public ApiResponse<CertificateTemplateResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateCertificateTemplateRequest request) {
		return ApiResponse.success(certificateTemplateMapper.toResponse(
				certificateTemplateService.updateDetails(publicId, request.name(), request.bodyTemplate())));
	}

	@Override
	@PatchMapping("/{publicId}/activate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_WRITE')")
	public ApiResponse<CertificateTemplateResponse> activate(@PathVariable String publicId) {
		return ApiResponse.success(certificateTemplateMapper.toResponse(certificateTemplateService.activate(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_WRITE')")
	public ApiResponse<CertificateTemplateResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse
				.success(certificateTemplateMapper.toResponse(certificateTemplateService.deactivate(publicId)));
	}
}
