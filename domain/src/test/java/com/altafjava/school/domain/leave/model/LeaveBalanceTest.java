package com.altafjava.school.domain.leave.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class LeaveBalanceTest {

	@Test
	void allocate_startsWithZeroUsedDays() {
		LeaveBalance balance = LeaveBalance.allocate(1L, 2L, 3L, BigDecimal.TEN);

		assertEquals(0, BigDecimal.TEN.compareTo(balance.remainingDays()));
	}

	@Test
	void deduct_withinRemaining_reducesRemainingDays() {
		LeaveBalance balance = LeaveBalance.allocate(1L, 2L, 3L, BigDecimal.TEN);

		balance.deduct(BigDecimal.valueOf(4));

		assertEquals(0, BigDecimal.valueOf(6).compareTo(balance.remainingDays()));
	}

	@Test
	void deduct_exceedingRemaining_throwsBusinessException() {
		LeaveBalance balance = LeaveBalance.allocate(1L, 2L, 3L, BigDecimal.valueOf(2));

		assertThrows(BusinessException.class, () -> balance.deduct(BigDecimal.valueOf(3)));
	}

	@Test
	void credit_reversesADeduction() {
		LeaveBalance balance = LeaveBalance.allocate(1L, 2L, 3L, BigDecimal.TEN);
		balance.deduct(BigDecimal.valueOf(4));

		balance.credit(BigDecimal.valueOf(4));

		assertEquals(0, BigDecimal.TEN.compareTo(balance.remainingDays()));
	}

	@Test
	void credit_neverPushesUsedDaysBelowZero() {
		LeaveBalance balance = LeaveBalance.allocate(1L, 2L, 3L, BigDecimal.TEN);

		balance.credit(BigDecimal.valueOf(4));

		assertEquals(0, BigDecimal.ZERO.compareTo(balance.getUsedDays()));
	}
}
