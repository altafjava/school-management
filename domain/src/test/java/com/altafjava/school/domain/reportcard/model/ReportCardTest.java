package com.altafjava.school.domain.reportcard.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class ReportCardTest {

	@Test
	void create_setsAllFields() {
		ReportCard reportCard = ReportCard.create(1L, 2L, "tenants/1/report-cards/1/2/abc.pdf");

		assertEquals(1L, reportCard.getStudentId());
		assertEquals(2L, reportCard.getTermId());
		assertEquals("tenants/1/report-cards/1/2/abc.pdf", reportCard.getStorageKey());
		assertNotNull(reportCard.getGeneratedAt());
	}
}
