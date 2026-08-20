package com.altafjava.school.application.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

/**
 * Write-side counterpart to {@link TeacherClassroomScopeResolver}'s read-side list narrowing:
 * resolves the current authenticated TEACHER's {@code Teacher.id} and asserts they are the
 * assigned class teacher of a given classroom, using the same
 * {@code Teacher#getUserId()} -> {@code Classroom#getClassTeacherId()} lookup idiom.
 */
@Component
public class TeacherClassroomMembershipGuard {

	private final TeacherRepository teacherRepository;
	private final ClassroomRepository classroomRepository;

	public TeacherClassroomMembershipGuard(TeacherRepository teacherRepository,
			ClassroomRepository classroomRepository) {
		this.teacherRepository = teacherRepository;
		this.classroomRepository = classroomRepository;
	}

	public Long assertTeachesClassroomAndResolveTeacherId(Long tenantId, Long classroomId) {
		Long userId = resolveUserId();
		Teacher teacher = teacherRepository.findByUserIdAndTenantId(userId, tenantId)
				.orElseThrow(() -> new AccessDeniedException("No teacher record linked to the current user"));
		boolean teachesClassroom = classroomRepository.findAllByClassTeacherIdAndTenantId(teacher.getId(), tenantId)
				.stream()
				.anyMatch(classroom -> classroom.getId().equals(classroomId));
		if (!teachesClassroom) {
			throw new AccessDeniedException("Teacher is not assigned to classroom " + classroomId);
		}
		return teacher.getId();
	}

	private Long resolveUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user.getId();
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot verify classroom access");
	}
}
