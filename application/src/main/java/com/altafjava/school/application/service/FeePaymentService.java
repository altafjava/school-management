package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class FeePaymentService {

	private final FeePaymentRepository feePaymentRepository;
	private final StudentRepository studentRepository;
	private final FeeStructureRepository feeStructureRepository;

	public FeePaymentService(FeePaymentRepository feePaymentRepository, StudentRepository studentRepository,
			FeeStructureRepository feeStructureRepository) {
		this.feePaymentRepository = feePaymentRepository;
		this.studentRepository = studentRepository;
		this.feeStructureRepository = feeStructureRepository;
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
