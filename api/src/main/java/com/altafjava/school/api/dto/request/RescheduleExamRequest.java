package com.altafjava.school.api.dto.request;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;

public record RescheduleExamRequest(@NotNull LocalDateTime scheduledAt) {
}
