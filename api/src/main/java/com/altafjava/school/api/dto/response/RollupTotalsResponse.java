package com.altafjava.school.api.dto.response;

public record RollupTotalsResponse(long activeStudentCount, AttendanceRollupResponse attendance,
		FeeRollupResponse fees) {
}
