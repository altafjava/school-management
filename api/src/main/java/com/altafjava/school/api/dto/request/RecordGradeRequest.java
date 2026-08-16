package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordGradeRequest(
		@NotNull Long studentId,
		@NotBlank @Size(max = 100) String subject,
		@NotNull Long examId,
		@NotNull @DecimalMin("0.0") BigDecimal marks,
		@Size(max = 100) String gradedBy) {
}
