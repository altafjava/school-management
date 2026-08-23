package com.altafjava.school.api.dto.response;

public record BookResponse(
		String publicId,
		String isbn,
		String title,
		String author,
		String publisher,
		String category,
		boolean active) {
}
