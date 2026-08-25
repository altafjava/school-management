package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.payroll.model.SalaryStructure;
import com.altafjava.school.domain.payroll.repository.SalaryStructureRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@Service
public class SalaryStructureService {

	private final SalaryStructureRepository salaryStructureRepository;
	private final TeacherRepository teacherRepository;

	public SalaryStructureService(SalaryStructureRepository salaryStructureRepository,
			TeacherRepository teacherRepository) {
		this.salaryStructureRepository = salaryStructureRepository;
		this.teacherRepository = teacherRepository;
	}

	@Transactional(readOnly = true)
	public Page<SalaryStructure> listForTeacher(String teacherPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Teacher teacher = findTeacher(tenantId, teacherPublicId);
		return salaryStructureRepository.findAllByTeacherIdAndTenantId(teacher.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public SalaryStructure findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return salaryStructureRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Salary structure not found: " + publicId));
	}

	@Transactional
	public SalaryStructure create(String teacherPublicId, BigDecimal basicPay, BigDecimal houseRentAllowance,
			BigDecimal transportAllowance, BigDecimal otherAllowances, BigDecimal otherDeductions,
			LocalDate effectiveFrom) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Teacher teacher = findTeacher(tenantId, teacherPublicId);
		return supersedeForTeacher(tenantId, teacher.getId(), basicPay, houseRentAllowance, transportAllowance,
				otherAllowances, otherDeductions, effectiveFrom);
	}

	@Transactional
	public SalaryStructure supersede(String currentStructurePublicId, BigDecimal basicPay,
			BigDecimal houseRentAllowance, BigDecimal transportAllowance, BigDecimal otherAllowances,
			BigDecimal otherDeductions, LocalDate effectiveFrom) {
		Long tenantId = TenantContext.getCurrentTenantId();
		SalaryStructure current = salaryStructureRepository
				.findByPublicIdAndTenantId(UUID.fromString(currentStructurePublicId), tenantId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Salary structure not found: " + currentStructurePublicId));
		return supersedeForTeacher(tenantId, current.getTeacherId(), basicPay, houseRentAllowance, transportAllowance,
				otherAllowances, otherDeductions, effectiveFrom);
	}

	/**
	 * At most one salary structure may be active per teacher. Deactivates any existing active
	 * structure before saving the new one, mirroring how {@code AcademicYearService} flips the
	 * previous {@code current} academic year.
	 */
	private SalaryStructure supersedeForTeacher(Long tenantId, Long teacherId, BigDecimal basicPay,
			BigDecimal houseRentAllowance, BigDecimal transportAllowance, BigDecimal otherAllowances,
			BigDecimal otherDeductions, LocalDate effectiveFrom) {
		salaryStructureRepository.findByTeacherIdAndActiveTrueAndTenantId(teacherId, tenantId)
				.ifPresent(existing -> {
					existing.deactivate();
					salaryStructureRepository.save(existing);
				});
		SalaryStructure structure = SalaryStructure.create(teacherId, basicPay, houseRentAllowance,
				transportAllowance, otherAllowances, otherDeductions, effectiveFrom);
		return salaryStructureRepository.save(structure);
	}

	private Teacher findTeacher(Long tenantId, String teacherPublicId) {
		return teacherRepository.findByPublicIdAndTenantId(UUID.fromString(teacherPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherPublicId));
	}
}
