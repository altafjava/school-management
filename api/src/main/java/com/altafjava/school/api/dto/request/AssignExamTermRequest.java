package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignExamTermRequest(@NotNull Long termId) {
}
