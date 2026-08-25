package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateHostelBuildingRequest(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 500) String address) {
}
