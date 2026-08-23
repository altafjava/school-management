package com.altafjava.school.api.dto.response;

public record VehicleResponse(
		String publicId,
		String registrationNumber,
		int capacity,
		String driverName,
		String driverContact,
		boolean active) {
}
