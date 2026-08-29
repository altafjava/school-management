package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.payroll.model.PayComponentAmount;
import com.altafjava.school.domain.payroll.model.PayComponentDefinition;
import com.altafjava.school.domain.payroll.model.SalaryStructure;
import com.altafjava.school.domain.payroll.repository.PayComponentDefinitionRepository;
import com.altafjava.school.domain.payroll.repository.SalaryStructureRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@Service
public class SalaryStructureService {

	private final SalaryStructureRepository salaryStructureRepository;
	private final TeacherRepository teacherRepository;
	private final PayComponentDefinitionRepository payComponentDefinitionRepository;

	public SalaryStructureService(SalaryStructureRepository salaryStructureRepository,
			TeacherRepository teacherRepository, PayComponentDefinitionRepository payComponentDefinitionRepository) {
		this.salaryStructureRepository = salaryStructureRepository;
		this.teacherRepository = teacherRepository;
		this.payComponentDefinitionRepository = payComponentDefinitionRepository;
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

	/**
	 * @param componentAmountsByCode
	 *                                   amount per {@code PayComponentDefinition.code}; name/type are
	 *                                   resolved from the tenant's current catalog, never trusted from
	 *                                   the caller.
	 */
	@Transactional
	public SalaryStructure create(String teacherPublicId, Map<String, BigDecimal> componentAmountsByCode,
			LocalDate effectiveFrom) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Teacher teacher = findTeacher(tenantId, teacherPublicId);
		return supersedeForTeacher(tenantId, teacher.getId(), componentAmountsByCode, effectiveFrom);
	}

	@Transactional
	public SalaryStructure supersede(String currentStructurePublicId, Map<String, BigDecimal> componentAmountsByCode,
			LocalDate effectiveFrom) {
		Long tenantId = TenantContext.getCurrentTenantId();
		SalaryStructure current = salaryStructureRepository
				.findByPublicIdAndTenantId(UUID.fromString(currentStructurePublicId), tenantId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Salary structure not found: " + currentStructurePublicId));
		return supersedeForTeacher(tenantId, current.getTeacherId(), componentAmountsByCode, effectiveFrom);
	}

	/**
	 * At most one salary structure may be active per teacher. Deactivates any existing active
	 * structure before saving the new one, mirroring how {@code AcademicYearService} flips the
	 * previous {@code current} academic year.
	 */
	private SalaryStructure supersedeForTeacher(Long tenantId, Long teacherId,
			Map<String, BigDecimal> componentAmountsByCode, LocalDate effectiveFrom) {
		List<PayComponentAmount> components = resolveComponents(tenantId, componentAmountsByCode);
		salaryStructureRepository.findByTeacherIdAndActiveTrueAndTenantId(teacherId, tenantId)
				.ifPresent(existing -> {
					existing.deactivate();
					salaryStructureRepository.save(existing);
				});
		SalaryStructure structure = SalaryStructure.create(teacherId, components, effectiveFrom);
		return salaryStructureRepository.save(structure);
	}

	// Each amount must reference a pay component the tenant has actually defined and kept active —
	// catches typos and stale codes from a component that was since renamed/deactivated, rather
	// than persisting an orphaned or spoofable code/name/type.
	private List<PayComponentAmount> resolveComponents(Long tenantId, Map<String, BigDecimal> componentAmountsByCode) {
		return componentAmountsByCode.entrySet().stream()
				.map(entry -> {
					PayComponentDefinition definition = payComponentDefinitionRepository
							.findByCodeAndTenantId(entry.getKey(), tenantId)
							.orElseThrow(() -> new BusinessException("Unknown pay component: " + entry.getKey()));
					if (!definition.isActive()) {
						throw new BusinessException("Pay component is not active: " + entry.getKey());
					}
					return new PayComponentAmount(definition.getCode(), definition.getName(), definition.getType(),
							entry.getValue());
				})
				.toList();
	}

	private Teacher findTeacher(Long tenantId, String teacherPublicId) {
		return teacherRepository.findByPublicIdAndTenantId(UUID.fromString(teacherPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherPublicId));
	}
}
