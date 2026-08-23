package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

public record ReturnBookRequest(@NotNull LocalDate returnedAt) {
}
