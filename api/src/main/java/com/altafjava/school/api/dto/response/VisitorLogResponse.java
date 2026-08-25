package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;

public record VisitorLogResponse(
		String publicId,
		String visitorName,
		String visitorPhone,
		String purpose,
		Long hostTeacherId,
		LocalDateTime checkInAt,
		LocalDateTime checkOutAt) {
}
