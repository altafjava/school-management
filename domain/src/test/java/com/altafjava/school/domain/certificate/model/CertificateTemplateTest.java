package com.altafjava.school.domain.certificate.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CertificateTemplateTest {

	@Test
	void create_setsNameBodyTemplateAndDefaultsActiveTrue() {
		CertificateTemplate template = CertificateTemplate.create("Bonafide Certificate",
				"This is to certify that {{studentName}} is a bonafide student.");

		assertEquals("Bonafide Certificate", template.getName());
		assertTrue(template.getBodyTemplate().contains("{{studentName}}"));
		assertTrue(template.isActive());
	}

	@Test
	void updateDetails_replacesNameAndBody() {
		CertificateTemplate template = CertificateTemplate.create("Old Name", "Old body");

		template.updateDetails("New Name", "New body {{studentName}}");

		assertEquals("New Name", template.getName());
		assertEquals("New body {{studentName}}", template.getBodyTemplate());
	}

	@Test
	void deactivateThenActivate_flipsActiveFlag() {
		CertificateTemplate template = CertificateTemplate.create("TC", "body");

		template.deactivate();
		assertFalse(template.isActive());

		template.activate();
		assertTrue(template.isActive());
	}
}
