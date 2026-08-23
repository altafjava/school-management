package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record TransportAssignmentResponse(
		String publicId,
		Long studentId,
		Long routeId,
		Long vehicleId,
		Long routeStopId,
		LocalDate effectiveFrom,
		LocalDate effectiveTo) {
}
