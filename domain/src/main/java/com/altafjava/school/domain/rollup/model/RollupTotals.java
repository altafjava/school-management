package com.altafjava.school.domain.rollup.model;

public record RollupTotals(long activeStudentCount, AttendanceRollup attendance, FeeRollup fees) {
}
