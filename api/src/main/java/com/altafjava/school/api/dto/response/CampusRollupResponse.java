package com.altafjava.school.api.dto.response;

public record CampusRollupResponse(String tenantPublicId, String tenantName, long activeStudentCount,
		AttendanceRollupResponse attendance, FeeRollupResponse fees) {
}
