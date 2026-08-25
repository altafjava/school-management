package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleCounselingSessionRequest(
		@NotBlank String studentPublicId,
		@NotBlank String counselorTeacherPublicId,
		@NotNull LocalDate sessionDate,
		@Size(max = 2000) String notes,
		boolean followUpRequired) {
}
