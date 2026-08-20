package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RecordEntranceTestScoreRequest(
		@NotNull @PositiveOrZero BigDecimal score,
		@NotNull @PositiveOrZero BigDecimal maxScore) {
}
