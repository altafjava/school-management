package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignTicketRequest(@NotNull Long assignedToUserId) {
}
