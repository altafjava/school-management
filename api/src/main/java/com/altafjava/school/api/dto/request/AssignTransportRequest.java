package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssignTransportRequest(
		@NotBlank String studentPublicId,
		@NotBlank String routePublicId,
		@NotBlank String vehiclePublicId,
		@NotBlank String routeStopPublicId,
		@NotNull LocalDate effectiveFrom) {
}
