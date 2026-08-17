package com.altafjava.school.domain.rollup.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrganizationRollupReport(UUID organizationPublicId, String organizationName, LocalDate periodStart,
		LocalDate periodEnd, List<CampusRollup> campuses, RollupTotals totals) {

	public OrganizationRollupReport {
		campuses = List.copyOf(campuses);
	}

	public static OrganizationRollupReport of(UUID organizationPublicId, String organizationName, LocalDate periodStart,
			LocalDate periodEnd, List<CampusRollup> campuses) {
		long totalActiveStudents = campuses.stream().mapToLong(CampusRollup::activeStudentCount).sum();
		AttendanceRollup totalAttendance = AttendanceRollup
				.sum(campuses.stream().map(CampusRollup::attendance).toList());
		FeeRollup totalFees = FeeRollup.sum(campuses.stream().map(CampusRollup::fees).toList());
		RollupTotals totals = new RollupTotals(totalActiveStudents, totalAttendance, totalFees);
		return new OrganizationRollupReport(organizationPublicId, organizationName, periodStart, periodEnd, campuses,
				totals);
	}
}
