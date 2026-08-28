package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeAssignmentResponse(
		String publicId,
		Long feeStructureId,
		String scope,
		Long studentId,
		Long classroomId,
		LocalDate dueDate,
		Integer graceDays,
		BigDecimal lateFeePercentage) {
}
