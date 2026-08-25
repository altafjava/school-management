package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record RoomAllocationResponse(
		String publicId,
		Long studentId,
		Long roomId,
		LocalDate allocatedFrom,
		LocalDate allocatedUntil,
		boolean active) {
}
