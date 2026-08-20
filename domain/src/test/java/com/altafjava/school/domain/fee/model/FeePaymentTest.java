package com.altafjava.school.domain.fee.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FeePaymentTest {

	@Test
	void create_defaultsToManualPaymentSourceWithNoGatewayFields() {
		FeePayment payment = FeePayment.create(1L, 2L, BigDecimal.valueOf(500), LocalDateTime.now(), "RCPT-1");

		assertEquals(PaymentSource.MANUAL, payment.getPaymentSource());
		assertNull(payment.getGatewayProviderType());
		assertNull(payment.getGatewayChargeReference());
	}

	@Test
	void recordFromGateway_setsGatewaySourceAndFields() {
		LocalDateTime paidAt = LocalDateTime.of(2026, 3, 1, 10, 0);

		FeePayment payment = FeePayment.recordFromGateway(1L, 2L, BigDecimal.valueOf(500), paidAt, "GTW-pi_123",
				"STRIPE", "pi_123");

		assertEquals(1L, payment.getStudentId());
		assertEquals(2L, payment.getFeeStructureId());
		assertEquals(0, BigDecimal.valueOf(500).compareTo(payment.getPaidAmount()));
		assertEquals(paidAt, payment.getPaidAt());
		assertEquals("GTW-pi_123", payment.getReceiptNumber());
		assertEquals(PaymentSource.GATEWAY, payment.getPaymentSource());
		assertEquals("STRIPE", payment.getGatewayProviderType());
		assertEquals("pi_123", payment.getGatewayChargeReference());
	}
}
