package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

public record ReturnAssetRequest(@NotNull LocalDate returnedAt) {
}
