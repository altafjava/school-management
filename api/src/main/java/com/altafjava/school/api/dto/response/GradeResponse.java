package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record GradeResponse(
		String publicId,
		Long studentId,
		Long subjectId,
		Long examId,
		BigDecimal marks,
		String gradeLetter,
		String gradedBy) {
}
