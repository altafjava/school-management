package com.altafjava.school.domain.certificate.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class CertificateIssuanceTest {

	@Test
	void create_setsAllFieldsAndStampsIssuedAt() {
		CertificateIssuance issuance = CertificateIssuance.create(1L, 2L, "aBc123XY", "tenants/1/certificates/key.pdf",
				9L);

		assertEquals(1L, issuance.getStudentId());
		assertEquals(2L, issuance.getCertificateTemplateId());
		assertEquals("aBc123XY", issuance.getVerificationCode());
		assertEquals("tenants/1/certificates/key.pdf", issuance.getStorageKey());
		assertEquals(9L, issuance.getIssuedByUserId());
		assertNotNull(issuance.getIssuedAt());
	}
}
