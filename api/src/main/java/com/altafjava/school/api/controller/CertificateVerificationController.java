package com.altafjava.school.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.api.dto.response.CertificateVerificationResponse;
import com.altafjava.school.api.mapper.CertificateVerificationMapper;
import com.altafjava.school.application.service.CertificateService;

/**
 * The standard "certificate verification" surface: given a verification code printed on a
 * certificate, confirm it was genuinely issued without exposing the PDF or other student PII.
 *
 * <p>
 * Deliberately a separate controller from {@link CertificateController} — its authorization
 * posture is the opposite of every other certificate endpoint, so keeping it isolated makes the
 * exception easy to spot in review.
 *
 * <p>
 * Genuinely anonymous, no {@code @PreAuthorize} — a third party verifying a certificate (an
 * employer, another institution) has no account on this tenant at all, same as
 * {@code AdmissionController#apply}. Access is granted purely by platform-saas's
 * {@code SecurityConfig} permitAll matcher on {@code /api/v1/certificates/verify/**}; still
 * exposes nothing except a yes/no confirmation plus non-sensitive fields.
 */
@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateVerificationController {

	private final CertificateService certificateService;
	private final CertificateVerificationMapper certificateVerificationMapper;

	public CertificateVerificationController(CertificateService certificateService,
			CertificateVerificationMapper certificateVerificationMapper) {
		this.certificateService = certificateService;
		this.certificateVerificationMapper = certificateVerificationMapper;
	}

	@GetMapping("/verify/{verificationCode}")
	public CertificateVerificationResponse verify(@PathVariable String verificationCode) {
		return certificateVerificationMapper.toResponse(certificateService.verify(verificationCode));
	}
}
