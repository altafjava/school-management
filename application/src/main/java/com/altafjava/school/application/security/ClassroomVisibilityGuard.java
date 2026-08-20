package com.altafjava.school.application.security;

import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.ResourceAccessPolicyEnforcer;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.application.policy.ResourceAction;
import com.altafjava.school.application.policy.ResourceType;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * Authorizes read access to classroom-scoped LMS content (lessons, assignments): TENANT_ADMIN is
 * always allowed; TEACHER is allowed only when assigned to the classroom (delegates to the
 * existing {@code SchoolResourceAccessPolicy} CLASSROOM/READ rule); PARENT/STUDENT are allowed
 * only if at least one student on the classroom roster is a student they can already view
 * (delegates to the same policy's STUDENT/READ rule, per roster member) — reuses the platform
 * {@link ResourceAccessPolicyEnforcer} rather than re-deriving guardian-link traversal here.
 */
@Component
public class ClassroomVisibilityGuard {

	// Bounds the roster scan for the PARENT/STUDENT visibility check — no school-saas classroom
	// roster is expected to approach this size; this is an authorization check, not a listing.
	private static final int MAX_ROSTER_SCAN = 1000;

	private static final String ROLE_TENANT_ADMIN = "ROLE_" + Roles.TENANT_ADMIN;
	private static final String ROLE_TEACHER = "ROLE_" + SchoolRoles.TEACHER;

	private final ResourceAccessPolicyEnforcer resourceAccessPolicyEnforcer;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentRepository studentRepository;

	public ClassroomVisibilityGuard(ResourceAccessPolicyEnforcer resourceAccessPolicyEnforcer,
			StudentClassroomLinkRepository studentClassroomLinkRepository, StudentRepository studentRepository) {
		this.resourceAccessPolicyEnforcer = resourceAccessPolicyEnforcer;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.studentRepository = studentRepository;
	}

	public void assertCanView(Long tenantId, String classroomPublicId, Long classroomId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (hasTenantAdminRole(authentication)) {
			return;
		}
		String userId = resolveUserId(authentication);
		if (hasTeacherRole(authentication)) {
			assertTeacherAssigned(tenantId, classroomPublicId, userId);
			return;
		}
		if (isVisibleViaRoster(tenantId, classroomId, userId)) {
			return;
		}
		throw new AccessDeniedException("Not authorized to view classroom " + classroomPublicId);
	}

	private void assertTeacherAssigned(Long tenantId, String classroomPublicId, String userId) {
		boolean allowed = resourceAccessPolicyEnforcer.isAllowed(userId, tenantId, ResourceType.CLASSROOM.name(),
				classroomPublicId, ResourceAction.READ.name());
		if (!allowed) {
			throw new AccessDeniedException("Teacher is not assigned to classroom " + classroomPublicId);
		}
	}

	private boolean isVisibleViaRoster(Long tenantId, Long classroomId, String userId) {
		return studentClassroomLinkRepository.findByClassroomId(tenantId, classroomId,
				PageRequest.of(0, MAX_ROSTER_SCAN))
				.getContent().stream()
				.map(link -> studentRepository.findByIdAndTenantId(link.getStudentId(), tenantId))
				.flatMap(Optional::stream)
				.map(student -> student.getPublicId().toString())
				.anyMatch(studentPublicId -> resourceAccessPolicyEnforcer.isAllowed(userId, tenantId,
						ResourceType.STUDENT.name(), studentPublicId, ResourceAction.READ.name()));
	}

	private boolean hasTenantAdminRole(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> ROLE_TENANT_ADMIN.equals(authority.getAuthority()));
	}

	private boolean hasTeacherRole(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> ROLE_TEACHER.equals(authority.getAuthority()));
	}

	private String resolveUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return String.valueOf(user.getId());
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot verify classroom access");
	}
}
