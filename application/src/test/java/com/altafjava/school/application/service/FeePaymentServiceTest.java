package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeePaymentServiceTest {

	@Mock
	private FeePaymentRepository feePaymentRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private FeeStructureRepository feeStructureRepository;

	private FeePaymentService feePaymentService;

	@BeforeEach
	void setUp() {
		feePaymentService = new FeePaymentService(feePaymentRepository, studentRepository, feeStructureRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void record_withNonExistentStudent_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> feePaymentService.record(99L, 1L, BigDecimal.valueOf(500), LocalDateTime.now(), "RCPT-001"));

		verify(feePaymentRepository, never()).save(any());
	}

	@Test
	void record_withNonExistentFeeStructure_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(feeStructureRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> feePaymentService.record(1L, 99L, BigDecimal.valueOf(500), LocalDateTime.now(), "RCPT-001"));

		verify(feePaymentRepository, never()).save(any());
	}

	@Test
	void record_duplicateReceiptNumber_throwsIllegalArgument() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(feeStructureRepository.existsByIdAndTenantId(2L, 1L)).thenReturn(true);
		when(feePaymentRepository.existsByReceiptNumberAndTenantId("RCPT-001", 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> feePaymentService.record(1L, 2L, BigDecimal.valueOf(500), LocalDateTime.now(), "RCPT-001"));
	}

	@Test
	void record_withValidReferences_succeeds() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(feeStructureRepository.existsByIdAndTenantId(2L, 1L)).thenReturn(true);
		when(feePaymentRepository.existsByReceiptNumberAndTenantId("RCPT-001", 1L)).thenReturn(false);
		when(feePaymentRepository.save(any(FeePayment.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> feePaymentService.record(1L, 2L, BigDecimal.valueOf(500), LocalDateTime.now(),
				"RCPT-001"));
	}
}
