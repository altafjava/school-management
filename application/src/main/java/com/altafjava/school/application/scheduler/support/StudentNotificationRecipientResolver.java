package com.altafjava.school.application.scheduler.support;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;

/**
 * Resolves who to notify about a student: their primary guardian if one has a login account,
 * falling back to any other linked guardian with one, then to the student's own account. Returns
 * empty if none of the above have ever been given a platform login.
 */
@Component
public class StudentNotificationRecipientResolver {

	private final StudentGuardianLinkRepository studentGuardianLinkRepository;
	private final GuardianRepository guardianRepository;

	public StudentNotificationRecipientResolver(StudentGuardianLinkRepository studentGuardianLinkRepository,
			GuardianRepository guardianRepository) {
		this.studentGuardianLinkRepository = studentGuardianLinkRepository;
		this.guardianRepository = guardianRepository;
	}

	public Optional<Long> resolve(Long tenantId, Student student) {
		List<StudentGuardianLink> links = studentGuardianLinkRepository.findByStudentId(tenantId, student.getId());

		Optional<Long> primaryGuardianUserId = links.stream()
				.filter(StudentGuardianLink::isPrimaryContact)
				.findFirst()
				.flatMap(link -> guardianUserId(tenantId, link));
		if (primaryGuardianUserId.isPresent()) {
			return primaryGuardianUserId;
		}

		Optional<Long> anyGuardianUserId = links.stream()
				.map(link -> guardianUserId(tenantId, link))
				.flatMap(Optional::stream)
				.findFirst();
		if (anyGuardianUserId.isPresent()) {
			return anyGuardianUserId;
		}

		return Optional.ofNullable(student.getUserId());
	}

	private Optional<Long> guardianUserId(Long tenantId, StudentGuardianLink link) {
		return guardianRepository.findByIdAndTenantId(link.getGuardianId(), tenantId)
				.map(Guardian::getUserId);
	}
}
