package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecordDisciplineActionRequest(@NotBlank @Size(max = 1000) String actionTaken) {
}
