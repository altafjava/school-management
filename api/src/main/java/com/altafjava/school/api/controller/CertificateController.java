package com.altafjava.school.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.response.CertificateIssuanceResponse;
import com.altafjava.school.api.mapper.CertificateIssuanceMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.CertificateService;
import com.altafjava.school.domain.certificate.model.CertificateIssuance;

// Issuance is admin/registrar-only (see CertificateTemplateController's Javadoc for the
// TENANT_ADMIN/PRINCIPAL role choice) — unlike report cards, a certificate is never self-service
// for a parent/student, it is always issued by staff on request.
@RestController
@RequestMapping("/api/v1/students/{studentPublicId}/certificates")
public class CertificateController {

	private final CertificateService certificateService;
	private final CertificateIssuanceMapper certificateIssuanceMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CertificateController(CertificateService certificateService,
			CertificateIssuanceMapper certificateIssuanceMapper, SpringDataPageableResolver pageableResolver) {
		this.certificateService = certificateService;
		this.certificateIssuanceMapper = certificateIssuanceMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_MANAGE')")
	public Page<CertificateIssuanceResponse> list(@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return certificateService.listForStudent(studentPublicId, pageableResolver.resolve(page, size))
				.map(certificateIssuanceMapper::toResponse);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_MANAGE')")
	public CertificateIssuanceResponse issue(@PathVariable String studentPublicId,
			@RequestParam String certificateTemplatePublicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		CertificateIssuance issuance = certificateService.issue(studentPublicId, certificateTemplatePublicId,
				user.getId());
		return certificateIssuanceMapper.toResponse(issuance);
	}

	@GetMapping("/{certificatePublicId}/download")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_MANAGE')")
	public ResponseEntity<byte[]> download(@PathVariable String studentPublicId,
			@PathVariable String certificatePublicId) {
		CertificateIssuance issuance = certificateService.findByPublicId(studentPublicId, certificatePublicId);
		byte[] pdf = certificateService.downloadPdf(issuance);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(certificatePublicId + ".pdf").build().toString())
				.body(pdf);
	}
}
