package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class StudentService {

	private final StudentRepository studentRepository;

	public StudentService(StudentRepository studentRepository) {
		this.studentRepository = studentRepository;
	}

	@Transactional(readOnly = true)
	public Page<Student> listStudents(Pageable pageable) {
		return studentRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<Student> listStudents(Pageable pageable, EnrollmentStatus status) {
		if (status == null) {
			return listStudents(pageable);
		}
		return studentRepository.findAllByTenantIdAndEnrollmentStatus(TenantContext.getCurrentTenantId(), status,
				pageable);
	}

	@Transactional(readOnly = true)
	public Student findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + publicId));
	}

	@Transactional
	public Student enroll(String studentCode, String firstName, String lastName,
			String email, LocalDate dateOfBirth) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (studentRepository.existsByStudentCodeAndTenantId(studentCode, tenantId)) {
			throw new BusinessException("Student code already exists: " + studentCode);
		}
		Student student = Student.create(studentCode, firstName, lastName, email, dateOfBirth);
		return studentRepository.save(student);
	}

	@Transactional
	public Student withdraw(String publicId) {
		Student student = findByPublicId(publicId);
		student.withdraw();
		return studentRepository.save(student);
	}

	@Transactional
	public Student graduate(String publicId) {
		Student student = findByPublicId(publicId);
		student.graduate();
		return studentRepository.save(student);
	}

	@Transactional
	public Student updateContactDetails(String publicId, String firstName, String lastName, String email,
			LocalDate dateOfBirth) {
		Student student = findByPublicId(publicId);
		student.updateContactDetails(firstName, lastName, email, dateOfBirth);
		return studentRepository.save(student);
	}
}
