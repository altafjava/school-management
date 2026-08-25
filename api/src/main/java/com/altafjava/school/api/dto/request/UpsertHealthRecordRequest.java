package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Size;

public record UpsertHealthRecordRequest(
		@Size(max = 10) String bloodGroup,
		@Size(max = 1000) String allergies,
		@Size(max = 1000) String conditions,
		@Size(max = 1000) String immunizations) {
}
