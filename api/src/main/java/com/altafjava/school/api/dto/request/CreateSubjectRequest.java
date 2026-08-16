package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
		@NotBlank @Size(max = 20) String code,
		@NotBlank @Size(max = 100) String name,
		@Size(max = 500) String description) {
}
