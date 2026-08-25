package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReferForCounselingRequest(
		@NotBlank String studentPublicId,
		@NotBlank @Size(max = 1000) String reason) {
}
