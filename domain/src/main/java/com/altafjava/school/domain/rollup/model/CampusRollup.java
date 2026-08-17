package com.altafjava.school.domain.rollup.model;

import java.util.UUID;

public record CampusRollup(UUID tenantPublicId, String tenantName, long activeStudentCount,
		AttendanceRollup attendance, FeeRollup fees) {
}
