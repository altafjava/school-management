package com.altafjava.school.domain.fee.model;

// Distinguishes staff-recorded payments from ones confirmed through the payment gateway
// integration — see FeePayment.create (MANUAL) vs FeePayment.recordFromGateway (GATEWAY).
public enum PaymentSource {
	MANUAL, GATEWAY
}
