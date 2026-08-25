package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoomRequest(
		@NotBlank @Size(max = 20) String roomNumber,
		@Min(1) int capacity) {
}
