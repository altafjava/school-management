package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

public record SetTeacherProbationRequest(@NotNull LocalDate probationEndDate) {
}
