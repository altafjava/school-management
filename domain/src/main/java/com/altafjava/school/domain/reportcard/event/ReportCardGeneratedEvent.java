package com.altafjava.school.domain.reportcard.event;

import java.time.Instant;

// Published after a ReportCard is persisted, for future domain listeners.
public record ReportCardGeneratedEvent(
		Long tenantId,
		Long studentId,
		Long termId,
		Long reportCardId,
		Instant timestamp) {

	public ReportCardGeneratedEvent(Long tenantId, Long studentId, Long termId, Long reportCardId) {
		this(tenantId, studentId, termId, reportCardId, Instant.now());
	}
}
