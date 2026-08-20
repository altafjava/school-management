package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.altafjava.school.domain.admission.model.AdmissionStatus;

public record AdmissionResponse(
		String publicId,
		String applicantFirstName,
		String applicantLastName,
		LocalDate applicantDateOfBirth,
		String guardianFirstName,
		String guardianLastName,
		String guardianEmail,
		String guardianPhone,
		String appliedGrade,
		AdmissionStatus status,
		Instant submittedAt,
		BigDecimal entranceTestScore,
		BigDecimal entranceTestMaxScore,
		Integer meritRank) {
}
