package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddBookCopyRequest(@NotBlank @Size(max = 50) String copyCode) {
}
