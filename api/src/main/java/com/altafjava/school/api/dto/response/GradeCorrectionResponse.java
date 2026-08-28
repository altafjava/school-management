package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record GradeCorrectionResponse(
		String publicId,
		BigDecimal oldMarks,
		String oldGradeLetter,
		BigDecimal newMarks,
		String newGradeLetter,
		String correctedBy,
		Instant correctedAt) {
}
