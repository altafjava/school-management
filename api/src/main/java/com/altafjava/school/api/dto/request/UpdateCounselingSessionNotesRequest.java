package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCounselingSessionNotesRequest(
		@Size(max = 2000) String notes,
		boolean followUpRequired) {
}
