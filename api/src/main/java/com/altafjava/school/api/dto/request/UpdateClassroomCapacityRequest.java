package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Min;

public record UpdateClassroomCapacityRequest(@Min(1) Integer capacity) {
}
