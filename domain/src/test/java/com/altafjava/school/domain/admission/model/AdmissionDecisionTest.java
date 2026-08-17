package com.altafjava.school.domain.admission.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class AdmissionDecisionTest {

	@Test
	void record_setsAllFields() {
		AdmissionDecision decision = AdmissionDecision.record(1L, DecisionOutcome.APPROVED, "admin", "looks good");

		assertEquals(1L, decision.getAdmissionId());
		assertEquals(DecisionOutcome.APPROVED, decision.getOutcome());
		assertEquals("admin", decision.getDecidedBy());
		assertEquals("looks good", decision.getNotes());
		assertNotNull(decision.getDecidedAt());
	}
}
