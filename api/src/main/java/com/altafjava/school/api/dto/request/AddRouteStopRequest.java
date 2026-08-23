package com.altafjava.school.api.dto.request;

import java.time.LocalTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddRouteStopRequest(
		@NotBlank String stopName,
		@NotNull Integer sequenceOrder,
		LocalTime pickupTime,
		LocalTime dropTime) {
}
