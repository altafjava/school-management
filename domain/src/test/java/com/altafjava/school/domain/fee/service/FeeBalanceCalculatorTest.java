package com.altafjava.school.domain.fee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;

class FeeBalanceCalculatorTest {

	private final FeeBalanceCalculator calculator = new FeeBalanceCalculator();

	private FeeStructure feeStructure(BigDecimal amount) {
		FeeStructure feeStructure = FeeStructure.create("Tuition", amount, FeeFrequency.MONTHLY, "Standard");
		feeStructure.setId(1L);
		return feeStructure;
	}

	@Test
	void calculate_noPayments_fullAmountOutstanding() {
		FeeBalance balance = calculator.calculate(feeStructure(BigDecimal.valueOf(1000)), null);

		assertEquals(BigDecimal.valueOf(1000), balance.outstandingBalance());
		assertEquals(BigDecimal.ZERO, balance.overpaidAmount());
		assertEquals(BigDecimal.ZERO, balance.amountPaid());
	}

	@Test
	void calculate_partialPayment_reducesOutstandingBalance() {
		FeeBalance balance = calculator.calculate(feeStructure(BigDecimal.valueOf(1000)), BigDecimal.valueOf(400));

		assertEquals(BigDecimal.valueOf(600), balance.outstandingBalance());
		assertEquals(BigDecimal.ZERO, balance.overpaidAmount());
	}

	@Test
	void calculate_exactPayment_zeroOutstandingAndZeroOverpaid() {
		FeeBalance balance = calculator.calculate(feeStructure(BigDecimal.valueOf(1000)), BigDecimal.valueOf(1000));

		assertEquals(BigDecimal.ZERO, balance.outstandingBalance());
		assertEquals(BigDecimal.ZERO, balance.overpaidAmount());
	}

	@Test
	void calculate_overpayment_reportsOverpaidAmountExplicitlyNotAsNegativeBalance() {
		FeeBalance balance = calculator.calculate(feeStructure(BigDecimal.valueOf(1000)), BigDecimal.valueOf(1200));

		assertEquals(BigDecimal.ZERO, balance.outstandingBalance());
		assertEquals(BigDecimal.valueOf(200), balance.overpaidAmount());
	}
}
