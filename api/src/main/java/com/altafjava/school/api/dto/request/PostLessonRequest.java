package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostLessonRequest(
		@NotBlank String classroomPublicId,
		@NotBlank String subjectPublicId,
		@NotBlank @Size(max = 200) String title,
		String description,
		@Size(max = 500) String storageKey) {
}
