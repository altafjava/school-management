package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SupersedeSalaryStructureRequest(
		@NotEmpty @Valid List<PayComponentAmountRequest> components,
		@NotNull LocalDate effectiveFrom) {

	public SupersedeSalaryStructureRequest {
		components = components == null ? null : List.copyOf(components);
	}
}
