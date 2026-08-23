package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;

public record EventResponse(
		String publicId,
		String title,
		String description,
		LocalDateTime eventDate,
		String location,
		boolean registrationRequired,
		Integer capacity,
		boolean active) {
}
