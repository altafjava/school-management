package com.altafjava.school.domain.library.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class CirculationTest {

	@Test
	void checkout_setsDueDate() {
		Circulation circulation = Circulation.checkout(1L, 2L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));

		assertEquals(LocalDate.of(2026, 4, 15), circulation.getDueDate());
	}

	@Test
	void returnBook_setsReturnedAtAndFine() {
		Circulation circulation = Circulation.checkout(1L, 2L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));

		circulation.returnBook(LocalDate.of(2026, 4, 20), BigDecimal.valueOf(25));

		assertEquals(LocalDate.of(2026, 4, 20), circulation.getReturnedAt());
		assertEquals(0, BigDecimal.valueOf(25).compareTo(circulation.getFineAmount()));
	}

	@Test
	void returnBook_alreadyReturned_throwsBusinessException() {
		Circulation circulation = Circulation.checkout(1L, 2L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));
		circulation.returnBook(LocalDate.of(2026, 4, 20), BigDecimal.ZERO);

		assertThrows(BusinessException.class,
				() -> circulation.returnBook(LocalDate.of(2026, 4, 21), BigDecimal.ZERO));
	}

	@Test
	void isOverdue_pastDueDateAndNotReturned_returnsTrue() {
		Circulation circulation = Circulation.checkout(1L, 2L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));

		assertTrue(circulation.isOverdue(LocalDate.of(2026, 4, 16)));
	}

	@Test
	void isOverdue_afterReturn_returnsFalse() {
		Circulation circulation = Circulation.checkout(1L, 2L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));
		circulation.returnBook(LocalDate.of(2026, 4, 20), BigDecimal.ZERO);

		assertFalse(circulation.isOverdue(LocalDate.of(2026, 4, 25)));
	}
}
