package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record GpaResponse(BigDecimal gpa, int gradeCount) {
}
