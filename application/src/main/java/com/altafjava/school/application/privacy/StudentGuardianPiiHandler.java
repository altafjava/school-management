package com.altafjava.school.application.privacy;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.privacy.DomainPiiHandler;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * school-saas's side of the GDPR/DPDP data-subject-request extension point — see
 * {@link DomainPiiHandler}, wired via {@code SchoolPlatformConfigurer#domainPiiHandler()}. A
 * subject may have a linked {@link Student} record, a linked {@link Guardian} record, both, or
 * neither ({@code userId} is nullable on both — see their own Javadoc); this handler checks both
 * independently rather than assuming exactly one exists.
 */
@Component
public class StudentGuardianPiiHandler implements DomainPiiHandler {

	private final StudentRepository studentRepository;
	private final GuardianRepository guardianRepository;

	public StudentGuardianPiiHandler(StudentRepository studentRepository, GuardianRepository guardianRepository) {
		this.studentRepository = studentRepository;
		this.guardianRepository = guardianRepository;
	}

	@Override
	@Transactional
	public void erase(Long tenantId, Long userId) {
		studentRepository.findByUserIdAndTenantId(userId, tenantId).ifPresent(student -> {
			student.erasePii();
			student.softDelete("gdpr-dsar-erasure");
			studentRepository.save(student);
		});
		guardianRepository.findByUserIdAndTenantId(userId, tenantId).ifPresent(guardian -> {
			guardian.erasePii();
			guardian.softDelete("gdpr-dsar-erasure");
			guardianRepository.save(guardian);
		});
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> export(Long tenantId, Long userId) {
		Map<String, Object> data = new HashMap<>();
		studentRepository.findByUserIdAndTenantId(userId, tenantId)
				.ifPresent(student -> data.put("student", toStudentExport(student)));
		guardianRepository.findByUserIdAndTenantId(userId, tenantId)
				.ifPresent(guardian -> data.put("guardian", toGuardianExport(guardian)));
		return data;
	}

	private Map<String, Object> toStudentExport(Student student) {
		Map<String, Object> export = new HashMap<>();
		export.put("publicId", student.getPublicId().toString());
		export.put("studentCode", student.getStudentCode());
		export.put("firstName", student.getFirstName());
		export.put("lastName", student.getLastName());
		export.put("email", student.getEmail());
		export.put("phone", student.getPhone());
		export.put("dateOfBirth", student.getDateOfBirth());
		return export;
	}

	private Map<String, Object> toGuardianExport(Guardian guardian) {
		Map<String, Object> export = new HashMap<>();
		export.put("publicId", guardian.getPublicId().toString());
		export.put("firstName", guardian.getFirstName());
		export.put("lastName", guardian.getLastName());
		export.put("email", guardian.getEmail());
		export.put("phone", guardian.getPhone());
		return export;
	}
}
