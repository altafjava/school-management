package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record CounselingSessionResponse(
		String publicId,
		Long studentId,
		Long counselorTeacherId,
		LocalDate sessionDate,
		String notes,
		boolean followUpRequired) {
}
