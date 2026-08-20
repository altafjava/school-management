package com.altafjava.school.domain.guardian.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class GuardianTest {

	private Guardian newGuardian() {
		return Guardian.create("Jane", "Doe", "jane@school.test", "555-0100", null);
	}

	@Test
	void linkUserAccount_whenUnlinked_setsUserId() {
		Guardian guardian = newGuardian();

		guardian.linkUserAccount(42L);

		assertEquals(42L, guardian.getUserId());
	}

	@Test
	void linkUserAccount_whenAlreadyLinked_throwsBusinessException() {
		Guardian guardian = newGuardian();
		guardian.linkUserAccount(42L);

		assertThrows(BusinessException.class, () -> guardian.linkUserAccount(99L));
	}
}
