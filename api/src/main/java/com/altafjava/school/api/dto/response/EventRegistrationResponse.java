package com.altafjava.school.api.dto.response;

public record EventRegistrationResponse(
		String publicId,
		Long eventId,
		Long studentId,
		String status) {
}
