package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.discipline.model.IncidentSeverity;

public record RecordDisciplineIncidentRequest(
		@NotBlank String studentPublicId,
		@NotNull LocalDate incidentDate,
		@NotNull IncidentSeverity severity,
		@NotBlank @Size(max = 1000) String description) {
}
