package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record DisciplineIncidentResponse(
		String publicId,
		Long studentId,
		Long reportedByTeacherId,
		LocalDate incidentDate,
		String severity,
		String description,
		String actionTaken,
		boolean guardianNotified) {
}
