package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Size(max = 50) String code,
		@Size(max = 500) String description) {
}
