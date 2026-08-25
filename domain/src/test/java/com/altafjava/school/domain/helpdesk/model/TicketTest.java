package com.altafjava.school.domain.helpdesk.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class TicketTest {

	private Ticket raised() {
		return Ticket.raise(1L, TicketCategory.TECHNICAL, "Cannot log in", "Login page shows a blank screen");
	}

	@Test
	void raise_setsFieldsAndDefaultsOpenStatus() {
		Ticket ticket = raised();

		assertEquals(1L, ticket.getRaisedByUserId());
		assertEquals(TicketCategory.TECHNICAL, ticket.getCategory());
		assertEquals("Cannot log in", ticket.getSubject());
		assertEquals(TicketStatus.OPEN, ticket.getStatus());
	}

	@Test
	void assign_fromOpen_setsAssigneeAndInProgressStatus() {
		Ticket ticket = raised();

		ticket.assign(99L);

		assertEquals(99L, ticket.getAssignedToUserId());
		assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
	}

	@Test
	void assign_whenClosed_throwsBusinessException() {
		Ticket ticket = raised();
		ticket.assign(99L);
		ticket.resolve("Reset the password");
		ticket.close();

		assertThrows(BusinessException.class, () -> ticket.assign(99L));
	}

	@Test
	void resolve_fromInProgress_setsResolvedStatusAndResolutionText() {
		Ticket ticket = raised();
		ticket.assign(99L);

		ticket.resolve("Reset the password");

		assertEquals(TicketStatus.RESOLVED, ticket.getStatus());
		assertEquals("Reset the password", ticket.getResolution());
	}

	@Test
	void resolve_whenAlreadyResolved_throwsBusinessException() {
		Ticket ticket = raised();
		ticket.resolve("Reset the password");

		assertThrows(BusinessException.class, () -> ticket.resolve("Again"));
	}

	@Test
	void close_fromResolved_setsClosedStatus() {
		Ticket ticket = raised();
		ticket.resolve("Reset the password");

		ticket.close();

		assertEquals(TicketStatus.CLOSED, ticket.getStatus());
	}

	@Test
	void close_whenNotResolved_throwsBusinessException() {
		Ticket ticket = raised();

		assertThrows(BusinessException.class, ticket::close);
	}

	@Test
	void reopen_fromClosed_setsOpenStatusAndClearsResolution() {
		Ticket ticket = raised();
		ticket.resolve("Reset the password");
		ticket.close();

		ticket.reopen();

		assertEquals(TicketStatus.OPEN, ticket.getStatus());
		assertNull(ticket.getResolution());
	}

	@Test
	void reopen_whenOpen_throwsBusinessException() {
		Ticket ticket = raised();

		assertThrows(BusinessException.class, ticket::reopen);
	}
}
