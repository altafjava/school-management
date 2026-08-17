package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitAdmissionRequest(
		@NotBlank @Size(max = 100) String applicantFirstName,
		@NotBlank @Size(max = 100) String applicantLastName,
		LocalDate applicantDateOfBirth,
		@NotBlank @Size(max = 100) String guardianFirstName,
		@NotBlank @Size(max = 100) String guardianLastName,
		@Email @Size(max = 255) String guardianEmail,
		@Size(max = 30) String guardianPhone,
		@NotBlank @Size(max = 20) String appliedGrade) {
}
