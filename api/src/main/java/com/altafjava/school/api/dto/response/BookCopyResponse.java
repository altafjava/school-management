package com.altafjava.school.api.dto.response;

public record BookCopyResponse(
		String publicId,
		Long bookId,
		String copyCode,
		String status) {
}
