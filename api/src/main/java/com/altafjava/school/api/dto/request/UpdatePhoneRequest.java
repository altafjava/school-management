package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Size;

public record UpdatePhoneRequest(@Size(max = 30) String phone) {
}
