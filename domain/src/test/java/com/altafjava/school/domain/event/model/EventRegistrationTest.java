package com.altafjava.school.domain.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class EventRegistrationTest {

	@Test
	void register_startsAsRegistered() {
		EventRegistration registration = EventRegistration.register(1L, 2L);

		assertEquals(EventRegistrationStatus.REGISTERED, registration.getStatus());
	}

	@Test
	void cancel_fromRegistered_succeeds() {
		EventRegistration registration = EventRegistration.register(1L, 2L);

		registration.cancel();

		assertEquals(EventRegistrationStatus.CANCELLED, registration.getStatus());
	}

	@Test
	void cancel_whenAlreadyCancelled_throwsBusinessException() {
		EventRegistration registration = EventRegistration.register(1L, 2L);
		registration.cancel();

		assertThrows(BusinessException.class, registration::cancel);
	}

	@Test
	void markAttended_fromRegistered_succeeds() {
		EventRegistration registration = EventRegistration.register(1L, 2L);

		registration.markAttended();

		assertEquals(EventRegistrationStatus.ATTENDED, registration.getStatus());
	}

	@Test
	void markAttended_whenCancelled_throwsBusinessException() {
		EventRegistration registration = EventRegistration.register(1L, 2L);
		registration.cancel();

		assertThrows(BusinessException.class, registration::markAttended);
	}
}
