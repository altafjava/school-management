package com.altafjava.school.api.dto.request;

public record ConfigureReportCardTemplateRequest(
		boolean showAttendanceSummary,
		boolean showRemarks,
		boolean showCompetencyGrid,
		boolean showRank) {
}
