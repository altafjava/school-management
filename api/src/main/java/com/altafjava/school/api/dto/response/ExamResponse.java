package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamResponse(
		String publicId,
		String title,
		Long subjectId,
		Long classroomId,
		LocalDateTime scheduledAt,
		BigDecimal maxMarks,
		Long termId,
		String status,
		String examType) {
}
