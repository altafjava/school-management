package com.altafjava.school.api.dto.response;

public record BoardResponse(
		String publicId,
		String name,
		String code,
		String description,
		boolean active) {
}
