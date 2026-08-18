package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EnrollStudentInClassroomRequest(
		@NotBlank String studentPublicId,
		@NotBlank String academicYearPublicId) {
}
