package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBookRequest(
		@Size(max = 20) String isbn,
		@NotBlank @Size(max = 250) String title,
		@Size(max = 150) String author,
		@Size(max = 150) String publisher,
		@Size(max = 100) String category) {
}
