package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.health.model.HealthRecord;
import com.altafjava.school.domain.health.repository.HealthRecordRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class HealthRecordService {

	private final HealthRecordRepository healthRecordRepository;
	private final StudentRepository studentRepository;

	public HealthRecordService(HealthRecordRepository healthRecordRepository, StudentRepository studentRepository) {
		this.healthRecordRepository = healthRecordRepository;
		this.studentRepository = studentRepository;
	}

	@Transactional(readOnly = true)
	public HealthRecord getByStudent(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(studentPublicId, tenantId);
		return healthRecordRepository.findByStudentIdAndTenantId(student.getId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Health record not found for student: "
						+ studentPublicId));
	}

	@Transactional
	public HealthRecord upsert(String studentPublicId, String bloodGroup, String allergies, String conditions,
			String immunizations) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(studentPublicId, tenantId);
		HealthRecord record = healthRecordRepository.findByStudentIdAndTenantId(student.getId(), tenantId)
				.orElse(null);
		if (record == null) {
			record = HealthRecord.create(student.getId(), bloodGroup, allergies, conditions, immunizations);
		} else {
			record.update(bloodGroup, allergies, conditions, immunizations);
		}
		return healthRecordRepository.save(record);
	}

	private Student resolveStudent(String studentPublicId, Long tenantId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}
}
