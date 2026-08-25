package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.helpdesk.model.TicketCategory;

public record RaiseTicketRequest(
		@NotNull TicketCategory category,
		@NotBlank @Size(max = 200) String subject,
		@NotBlank @Size(max = 2000) String description) {
}
