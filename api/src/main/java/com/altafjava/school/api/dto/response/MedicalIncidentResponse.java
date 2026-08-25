package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;

public record MedicalIncidentResponse(
		String publicId,
		Long studentId,
		LocalDateTime occurredAt,
		String description,
		String treatmentGiven,
		boolean guardianNotified,
		Long recordedByUserId) {
}
