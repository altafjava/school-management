package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCertificateTemplateRequest(
		@NotBlank @Size(max = 150) String name,
		@NotBlank String bodyTemplate) {
}
