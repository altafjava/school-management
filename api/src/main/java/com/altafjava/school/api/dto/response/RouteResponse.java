package com.altafjava.school.api.dto.response;

public record RouteResponse(
		String publicId,
		String name,
		String code,
		String description,
		boolean active) {
}
