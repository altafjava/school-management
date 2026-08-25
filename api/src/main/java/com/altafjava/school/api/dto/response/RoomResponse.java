package com.altafjava.school.api.dto.response;

public record RoomResponse(
		String publicId,
		Long hostelBuildingId,
		String roomNumber,
		int capacity,
		boolean active) {
}
