package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AllocateRoomRequest(
		@NotBlank String studentPublicId,
		@NotBlank String roomPublicId,
		@NotNull LocalDate allocatedFrom) {
}
