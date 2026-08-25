package com.altafjava.school.api.dto.response;

public record HostelBuildingResponse(
		String publicId,
		String name,
		String address,
		boolean active) {
}
