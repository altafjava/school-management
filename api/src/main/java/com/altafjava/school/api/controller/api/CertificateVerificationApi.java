package com.altafjava.school.api.controller.api;

import org.springframework.web.bind.annotation.PathVariable;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.response.CertificateVerificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Certificate Verification", description = "Genuinely anonymous, unauthenticated (permitAll) endpoint — "
		+ "a third party verifying a certificate (an employer, another institution) has no account and no tenant "
		+ "context; the tenant is resolved from the verification code itself.")
public interface CertificateVerificationApi {

	@Operation(summary = "Verify", operationId = "certificateverification_verify", description = "Looks up a certificate by its public verification code and confirms it was genuinely "
			+ "issued by this system, without exposing any other student data.")
	public ApiResponse<CertificateVerificationResponse> verify(@PathVariable String verificationCode);
}
