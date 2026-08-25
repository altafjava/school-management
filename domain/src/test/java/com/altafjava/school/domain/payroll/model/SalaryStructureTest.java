package com.altafjava.school.domain.payroll.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class SalaryStructureTest {

	private SalaryStructure structure() {
		return SalaryStructure.create(1L, BigDecimal.valueOf(50000), BigDecimal.valueOf(10000),
				BigDecimal.valueOf(2000), BigDecimal.valueOf(500), BigDecimal.valueOf(1000),
				LocalDate.of(2026, 1, 1));
	}

	@Test
	void create_isActiveByDefault() {
		SalaryStructure structure = structure();

		assertTrue(structure.isActive());
		assertEquals(1L, structure.getTeacherId());
	}

	@Test
	void create_withZeroBasicPay_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> SalaryStructure.create(1L, BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 1, 1)));
	}

	@Test
	void create_withNegativeBasicPay_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> SalaryStructure.create(1L, BigDecimal.valueOf(-100),
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 1, 1)));
	}

	@Test
	void deactivate_flipsActiveFlag() {
		SalaryStructure structure = structure();

		structure.deactivate();

		assertFalse(structure.isActive());
	}

	@Test
	void grossPay_sumsBasicAndAllowances() {
		SalaryStructure structure = structure();

		BigDecimal grossPay = structure.grossPay();

		assertEquals(0, BigDecimal.valueOf(62500).compareTo(grossPay));
	}

	@Test
	void toSnapshot_copiesAllLineItems() {
		SalaryStructure structure = structure();

		SalarySnapshot snapshot = structure.toSnapshot();

		assertEquals(0, structure.getBasicPay().compareTo(snapshot.basicPay()));
		assertEquals(0, structure.getHouseRentAllowance().compareTo(snapshot.houseRentAllowance()));
		assertEquals(0, structure.getTransportAllowance().compareTo(snapshot.transportAllowance()));
		assertEquals(0, structure.getOtherAllowances().compareTo(snapshot.otherAllowances()));
		assertEquals(0, structure.getOtherDeductions().compareTo(snapshot.otherDeductions()));
	}
}
