package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssignmentRequest(
		@NotBlank String classroomPublicId,
		@NotBlank String subjectPublicId,
		@NotBlank @Size(max = 200) String title,
		String description,
		@Size(max = 500) String storageKey,
		@NotNull LocalDate dueDate,
		@DecimalMin("0.01") BigDecimal maxMarks) {
}
