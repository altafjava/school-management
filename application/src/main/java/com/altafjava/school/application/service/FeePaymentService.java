package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.fee.service.FeeBalanceCalculator;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class FeePaymentService {

	private final FeePaymentRepository feePaymentRepository;
	private final StudentRepository studentRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final FeeBalanceCalculator feeBalanceCalculator = new FeeBalanceCalculator();

	public FeePaymentService(FeePaymentRepository feePaymentRepository, StudentRepository studentRepository,
			FeeStructureRepository feeStructureRepository, StudentDataAccessGuard studentDataAccessGuard) {
		this.feePaymentRepository = feePaymentRepository;
		this.studentRepository = studentRepository;
		this.feeStructureRepository = feeStructureRepository;
		this.studentDataAccessGuard = studentDataAccessGuard;
	}

	@Transactional(readOnly = true)
	public Page<FeePayment> listFeePayments(Pageable pageable) {
		return feePaymentRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public FeePayment findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return feePaymentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeePayment not found: " + publicId));
	}

	@Transactional(readOnly = true)
	public List<FeeBalance> calculateBalance(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return calculateBalanceForStudent(tenantId, student);
	}

	/**
	 * Same computation as {@link #calculateBalance}, for trusted internal callers (scheduler
	 * jobs) that already have a resolved {@link Student} and tenant, and are not acting on behalf
	 * of an end user — so no {@link StudentDataAccessGuard} check applies.
	 */
	@Transactional(readOnly = true)
	public List<FeeBalance> calculateBalanceForStudent(Long tenantId, Student student) {
		List<FeeStructure> feeStructures = feeStructureRepository.findAllByTenantId(tenantId);
		List<FeePayment> payments = feePaymentRepository.findByStudentId(tenantId, student.getId());

		return feeStructures.stream()
				.map(feeStructure -> feeBalanceCalculator.calculate(feeStructure,
						totalPaidFor(payments, feeStructure.getId())))
				.toList();
	}

	private BigDecimal totalPaidFor(List<FeePayment> payments, Long feeStructureId) {
		return payments.stream()
				.filter(payment -> feeStructureId.equals(payment.getFeeStructureId()))
				.map(FeePayment::getPaidAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Transactional
	public FeePayment record(Long studentId, Long feeStructureId, BigDecimal paidAmount,
			LocalDateTime paidAt, String receiptNumber) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!studentRepository.existsByIdAndTenantId(studentId, tenantId)) {
			throw new ResourceNotFoundException("Student not found: " + studentId);
		}
		if (!feeStructureRepository.existsByIdAndTenantId(feeStructureId, tenantId)) {
			throw new ResourceNotFoundException("FeeStructure not found: " + feeStructureId);
		}
		if (feePaymentRepository.existsByReceiptNumberAndTenantId(receiptNumber, tenantId)) {
			throw new IllegalArgumentException("Receipt number already exists: " + receiptNumber);
		}
		FeePayment payment = FeePayment.create(studentId, feeStructureId, paidAmount, paidAt, receiptNumber);
		return feePaymentRepository.save(payment);
	}
}
