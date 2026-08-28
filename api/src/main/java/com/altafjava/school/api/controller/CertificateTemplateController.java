package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
import com.altafjava.school.api.dto.request.CreateCertificateTemplateRequest;
import com.altafjava.school.api.dto.request.UpdateCertificateTemplateRequest;
import com.altafjava.school.api.dto.response.CertificateTemplateResponse;
import com.altafjava.school.api.mapper.CertificateTemplateMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.CertificateTemplateService;

// Tenant-admin-defined certificate catalog (e.g. "Bonafide Certificate", "Transfer Certificate").
// No dedicated REGISTRAR role exists in this codebase, so "admin/registrar" access is granted to
// TENANT_ADMIN (full CRUD) and PRINCIPAL (read-only), the closest existing school-admin role,
// mirroring how PRINCIPAL is already used for other admin-adjacent read surfaces.
@RestController
@RequestMapping("/api/v1/certificate-templates")
public class CertificateTemplateController {

	private final CertificateTemplateService certificateTemplateService;
	private final CertificateTemplateMapper certificateTemplateMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CertificateTemplateController(CertificateTemplateService certificateTemplateService,
			CertificateTemplateMapper certificateTemplateMapper, SpringDataPageableResolver pageableResolver) {
		this.certificateTemplateService = certificateTemplateService;
		this.certificateTemplateMapper = certificateTemplateMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_READ')")
	public Page<CertificateTemplateResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return certificateTemplateService.list(pageableResolver.resolve(page, size))
				.map(certificateTemplateMapper::toResponse);
	}

	@GetMapping("/active")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_READ')")
	public List<CertificateTemplateResponse> listActive() {
		return certificateTemplateService.listActive().stream().map(certificateTemplateMapper::toResponse).toList();
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_READ')")
	public CertificateTemplateResponse get(@PathVariable String publicId) {
		return certificateTemplateMapper.toResponse(certificateTemplateService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_WRITE')")
	public CertificateTemplateResponse create(@Valid @RequestBody CreateCertificateTemplateRequest request) {
		return certificateTemplateMapper
				.toResponse(certificateTemplateService.create(request.name(), request.bodyTemplate()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_WRITE')")
	public CertificateTemplateResponse updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateCertificateTemplateRequest request) {
		return certificateTemplateMapper.toResponse(
				certificateTemplateService.updateDetails(publicId, request.name(), request.bodyTemplate()));
	}

	@PatchMapping("/{publicId}/activate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_WRITE')")
	public CertificateTemplateResponse activate(@PathVariable String publicId) {
		return certificateTemplateMapper.toResponse(certificateTemplateService.activate(publicId));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_TEMPLATE_WRITE')")
	public CertificateTemplateResponse deactivate(@PathVariable String publicId) {
		return certificateTemplateMapper.toResponse(certificateTemplateService.deactivate(publicId));
	}
}
