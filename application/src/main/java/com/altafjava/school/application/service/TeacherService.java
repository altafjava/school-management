package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.teacher.model.EmploymentType;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@Service
public class TeacherService {

	private final TeacherRepository teacherRepository;
	private final DepartmentRepository departmentRepository;

	public TeacherService(TeacherRepository teacherRepository, DepartmentRepository departmentRepository) {
		this.teacherRepository = teacherRepository;
		this.departmentRepository = departmentRepository;
	}

	@Transactional(readOnly = true)
	public Page<Teacher> listTeachers(Pageable pageable) {
		return teacherRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Teacher findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return teacherRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + publicId));
	}

	@Transactional
	public Teacher hire(String employeeCode, String firstName, String lastName,
			String email, LocalDate joinDate) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (teacherRepository.existsByEmployeeCodeAndTenantId(employeeCode, tenantId)) {
			throw new IllegalArgumentException("Employee code already exists: " + employeeCode);
		}
		Teacher teacher = Teacher.create(employeeCode, firstName, lastName, email, joinDate);
		return teacherRepository.save(teacher);
	}

	@Transactional
	public Teacher updateContactDetails(String publicId, String firstName, String lastName, String email) {
		Teacher teacher = findByPublicId(publicId);
		teacher.updateContactDetails(firstName, lastName, email);
		return teacherRepository.save(teacher);
	}

	@Transactional
	public Teacher updateHrDetails(String publicId, String departmentPublicId, String qualification,
			EmploymentType employmentType) {
		Teacher teacher = findByPublicId(publicId);
		Long departmentId = resolveDepartmentId(departmentPublicId);
		teacher.assignHrDetails(departmentId, qualification, employmentType);
		return teacherRepository.save(teacher);
	}

	private Long resolveDepartmentId(String departmentPublicId) {
		if (departmentPublicId == null) {
			return null;
		}
		Long tenantId = TenantContext.getCurrentTenantId();
		return departmentRepository.findByPublicIdAndTenantId(UUID.fromString(departmentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentPublicId))
				.getId();
	}
}
