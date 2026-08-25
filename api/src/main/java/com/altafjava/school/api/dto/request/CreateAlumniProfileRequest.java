package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAlumniProfileRequest(
		@NotBlank String studentPublicId,
		@NotNull @Min(1900) @Max(2200) Integer graduationYear,
		@Size(max = 255) String currentOccupation,
		@Email @Size(max = 255) String contactEmail,
		@Size(max = 50) String contactPhone) {
}
