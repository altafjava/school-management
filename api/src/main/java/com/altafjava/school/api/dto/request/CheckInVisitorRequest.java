package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckInVisitorRequest(
		@NotBlank @Size(max = 150) String visitorName,
		@Size(max = 50) String visitorPhone,
		@NotBlank @Size(max = 500) String purpose,
		@NotBlank String hostTeacherPublicId) {
}
