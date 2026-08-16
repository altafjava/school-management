package com.altafjava.school.api.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.altafjava.school.api.dto.request.RecordGradeRequest;

class RecordGradeRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<ConstraintViolation<RecordGradeRequest>> violationsFor(RecordGradeRequest req) {
		return validator.validate(req);
	}

	private RecordGradeRequest valid() {
		return new RecordGradeRequest(1L, 10L, new BigDecimal("85.50"), "teacher@school.com");
	}

	@Test
	void valid_request_passesAllConstraints() {
		assertTrue(violationsFor(valid()).isEmpty());
	}

	@Test
	void studentId_null_failsValidation() {
		var req = new RecordGradeRequest(null, 10L, new BigDecimal("85.50"), null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void examId_null_failsValidation() {
		var req = new RecordGradeRequest(1L, null, new BigDecimal("85.50"), null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void marks_null_failsValidation() {
		var req = new RecordGradeRequest(1L, 10L, null, null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void marks_negative_failsDecimalMinValidation() {
		var req = new RecordGradeRequest(1L, 10L, new BigDecimal("-1.0"), null);
		assertFalse(violationsFor(req).isEmpty());
	}

	@Test
	void marks_zero_passesValidation() {
		// @DecimalMin("0.0") inclusive — zero is valid
		var req = new RecordGradeRequest(1L, 10L, BigDecimal.ZERO, null);
		assertTrue(violationsFor(req).isEmpty());
	}

	@Test
	void gradedBy_null_passesValidation() {
		// gradedBy has no @NotNull — it is optional
		var req = new RecordGradeRequest(1L, 10L, new BigDecimal("85.50"), null);
		assertTrue(violationsFor(req).isEmpty());
	}

	@Test
	void gradedBy_tooLong_failsValidation() {
		var req = new RecordGradeRequest(1L, 10L, new BigDecimal("85.50"), "T".repeat(101));
		assertFalse(violationsFor(req).isEmpty());
	}
}
