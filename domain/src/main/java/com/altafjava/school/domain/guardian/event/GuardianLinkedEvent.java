package com.altafjava.school.domain.guardian.event;

import java.time.Instant;
import com.altafjava.school.domain.guardian.model.RelationshipType;

// Published after a StudentGuardianLink is persisted, for future domain listeners.
public record GuardianLinkedEvent(
		Long tenantId,
		Long studentId,
		Long guardianId,
		RelationshipType relationshipType,
		Instant timestamp) {

	public GuardianLinkedEvent(Long tenantId, Long studentId, Long guardianId, RelationshipType relationshipType) {
		this(tenantId, studentId, guardianId, relationshipType, Instant.now());
	}
}
