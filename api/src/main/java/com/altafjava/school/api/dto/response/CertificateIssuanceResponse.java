package com.altafjava.school.api.dto.response;

import java.time.Instant;

public record CertificateIssuanceResponse(
		String publicId,
		Long studentId,
		Long certificateTemplateId,
		Instant issuedAt,
		String verificationCode) {
}
