package com.altafjava.school.api.dto.response;

public record FeeAssignmentResponse(
		String publicId,
		Long feeStructureId,
		String scope,
		Long studentId,
		Long classroomId) {
}
