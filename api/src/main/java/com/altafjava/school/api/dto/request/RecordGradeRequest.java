package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordGradeRequest(
		@NotNull Long studentId,
		@NotNull Long examId,
		@NotNull @DecimalMin("0.0") BigDecimal marks,
		@Size(max = 100) String gradedBy) {
}
