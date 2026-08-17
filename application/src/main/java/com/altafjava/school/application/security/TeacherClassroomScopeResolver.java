package com.altafjava.school.application.security;

import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

/**
 * Resolves which classrooms, if any, the current caller's TEACHER role should be scoped to.
 *
 * <p>
 * Bridges the authenticated platform user (a {@code users.id}) to the school-domain
 * {@code Teacher} that user is linked to via {@link Teacher#getUserId()} (added Phase 2), then to
 * the classrooms where that teacher is the assigned class teacher. TENANT_ADMIN is never scoped —
 * {@link #resolveClassroomIdsIfTeacherScoped} returns {@code Optional.empty()} for it, meaning "no
 * narrowing." Every other caller always gets {@code Optional.of(...)} — an empty list, not empty
 * Optional, when no Teacher/classroom link can be resolved — so an inconsistent or not-yet-linked
 * TEACHER account fails closed (sees nothing) rather than open (sees everything).
 */
@Component
public class TeacherClassroomScopeResolver {

	private static final String ROLE_TENANT_ADMIN = "ROLE_" + Roles.TENANT_ADMIN;

	private final TeacherRepository teacherRepository;
	private final ClassroomRepository classroomRepository;

	public TeacherClassroomScopeResolver(TeacherRepository teacherRepository,
			ClassroomRepository classroomRepository) {
		this.teacherRepository = teacherRepository;
		this.classroomRepository = classroomRepository;
	}

	public Optional<List<Long>> resolveClassroomIdsIfTeacherScoped(Long tenantId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (isTenantAdmin(authentication)) {
			return Optional.empty();
		}
		Long userId = resolveUserId(authentication);
		if (userId == null) {
			return Optional.of(List.of());
		}
		List<Long> classroomIds = teacherRepository.findByUserIdAndTenantId(userId, tenantId)
				.map(teacher -> classroomRepository.findAllByClassTeacherIdAndTenantId(teacher.getId(), tenantId)
						.stream()
						.map(Classroom::getId)
						.toList())
				.orElseGet(List::of);
		return Optional.of(classroomIds);
	}

	private boolean isTenantAdmin(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> ROLE_TENANT_ADMIN.equals(authority.getAuthority()));
	}

	private Long resolveUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user.getId();
		}
		return null;
	}
}
