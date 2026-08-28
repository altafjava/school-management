package com.altafjava.school.api.dto.response;

public record ReportCardTemplateResponse(
		boolean showAttendanceSummary,
		boolean showRemarks,
		boolean showCompetencyGrid,
		boolean showRank) {
}
