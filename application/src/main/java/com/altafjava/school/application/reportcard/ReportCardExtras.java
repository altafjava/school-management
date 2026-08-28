package com.altafjava.school.application.reportcard;

import java.util.List;
import com.altafjava.school.application.customfield.CustomFieldValue;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;

/**
 * Everything {@code ReportCardPdfGenerator} needs for its optional, tenant-configured sections —
 * bundled into one object. The four {@code showX} flags (extracted from {@code ReportCardTemplate}
 * by the caller, rather than holding that mutable JPA entity here) are the sole gate on whether a
 * section renders; a data field left {@code null}/empty simply means that section has nothing to
 * show, not that it's disabled.
 */
public record ReportCardExtras(
		boolean showAttendanceSummary,
		boolean showRemarks,
		boolean showCompetencyGrid,
		boolean showRank,
		AttendancePercentage attendancePercentage,
		Integer rank,
		List<CustomFieldValue> competencyValues,
		String classroomGrade,
		String classroomSection,
		String teacherRemarks,
		String principalRemarks) {

	public ReportCardExtras {
		competencyValues = competencyValues == null ? List.of() : List.copyOf(competencyValues);
	}
}
