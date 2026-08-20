package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record GradeSubmissionRequest(
		@NotNull @DecimalMin("0.0") BigDecimal marks,
		String feedback) {
}
