package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Deliberately its own DTO, not CreateGuardianRequest — this is the unauthenticated, public
// self-registration shape (credentials, no userId field) and must never accept caller-supplied
// roles or an explicit userId.
public record GuardianSelfRegisterRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank String password,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@Size(max = 30) String phone) {
}
