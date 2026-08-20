package com.altafjava.school.domain.attendance.model;

import java.math.BigDecimal;

public record AttendancePercentage(long presentDays, long totalMarkedDays, BigDecimal percentage) {
}
