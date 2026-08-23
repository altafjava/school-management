package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record AssetAssignmentResponse(
		String publicId,
		Long assetId,
		String assignedToType,
		Long assignedToId,
		LocalDate assignedAt,
		LocalDate returnedAt) {
}
