package com.altafjava.school.domain.rollup.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class FeeRollupTest {

	@Test
	void of_noPayments_fullAmountOutstanding() {
		FeeRollup rollup = FeeRollup.of(BigDecimal.valueOf(1000), null);

		assertEquals(BigDecimal.valueOf(1000), rollup.outstandingBalance());
		assertEquals(BigDecimal.ZERO, rollup.overpaidAmount());
		assertEquals(BigDecimal.ZERO, rollup.totalPaid());
	}

	@Test
	void of_partialPayment_reducesOutstandingBalance() {
		FeeRollup rollup = FeeRollup.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(400));

		assertEquals(BigDecimal.valueOf(600), rollup.outstandingBalance());
		assertEquals(BigDecimal.ZERO, rollup.overpaidAmount());
	}

	@Test
	void of_exactPayment_zeroOutstandingAndZeroOverpaid() {
		FeeRollup rollup = FeeRollup.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1000));

		assertEquals(BigDecimal.ZERO, rollup.outstandingBalance());
		assertEquals(BigDecimal.ZERO, rollup.overpaidAmount());
	}

	@Test
	void of_overpayment_reportsOverpaidAmountExplicitlyNotAsNegativeBalance() {
		FeeRollup rollup = FeeRollup.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1200));

		assertEquals(BigDecimal.ZERO, rollup.outstandingBalance());
		assertEquals(BigDecimal.valueOf(200), rollup.overpaidAmount());
	}

	@Test
	void sum_combinesMultipleCampusesAcrossAllFields() {
		FeeRollup campusA = FeeRollup.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(400));
		FeeRollup campusB = FeeRollup.of(BigDecimal.valueOf(500), BigDecimal.valueOf(700));

		FeeRollup total = FeeRollup.sum(List.of(campusA, campusB));

		assertEquals(BigDecimal.valueOf(1500), total.totalDue());
		assertEquals(BigDecimal.valueOf(1100), total.totalPaid());
		assertEquals(BigDecimal.valueOf(600), total.outstandingBalance());
		assertEquals(BigDecimal.valueOf(200), total.overpaidAmount());
	}

	@Test
	void sum_emptyList_returnsZero() {
		FeeRollup total = FeeRollup.sum(List.of());

		assertEquals(FeeRollup.ZERO, total);
	}
}
