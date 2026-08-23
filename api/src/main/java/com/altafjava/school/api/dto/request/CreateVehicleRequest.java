package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVehicleRequest(
		@NotBlank @Size(max = 50) String registrationNumber,
		@Min(1) int capacity,
		@Size(max = 100) String driverName,
		@Size(max = 50) String driverContact) {
}
