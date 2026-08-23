package com.altafjava.school.api.dto.request;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 1000) String description,
		@NotNull LocalDateTime eventDate,
		@Size(max = 200) String location,
		boolean registrationRequired,
		@Min(1) Integer capacity) {
}
