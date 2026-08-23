package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitLeaveRequestRequest(
		@NotBlank String leaveTypePublicId,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate,
		@Size(max = 500) String reason) {
}
