package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Size;

public record SubmitAssignmentRequest(
		@Size(max = 500) String storageKey,
		String textContent) {
}
