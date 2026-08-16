package com.altafjava.school.application.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.ResourceAccessPolicyEnforcer;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.application.policy.ResourceAction;
import com.altafjava.school.application.policy.ResourceType;

// Enforces "own child/own record only" on student-scoped reads; TENANT_ADMIN/TEACHER bypass it.
@Component
public class StudentDataAccessGuard {

	// "ROLE_" added manually since this reads GrantedAuthority directly, not via hasRole() SpEL.
	private static final String ROLE_TENANT_ADMIN = "ROLE_" + Roles.TENANT_ADMIN;
	private static final String ROLE_TEACHER = "ROLE_" + SchoolRoles.TEACHER;

	private final ResourceAccessPolicyEnforcer resourceAccessPolicyEnforcer;

	public StudentDataAccessGuard(ResourceAccessPolicyEnforcer resourceAccessPolicyEnforcer) {
		this.resourceAccessPolicyEnforcer = resourceAccessPolicyEnforcer;
	}

	public void assertCanView(Long tenantId, String studentPublicId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (hasStaffAccess(authentication)) {
			return;
		}
		String userId = resolveUserId(authentication);
		boolean allowed = resourceAccessPolicyEnforcer.isAllowed(userId, tenantId, ResourceType.STUDENT.name(),
				studentPublicId, ResourceAction.READ.name());
		if (!allowed) {
			throw new AccessDeniedException("Not authorized to view student " + studentPublicId + "'s data");
		}
	}

	private boolean hasStaffAccess(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> ROLE_TENANT_ADMIN.equals(authority.getAuthority())
						|| ROLE_TEACHER.equals(authority.getAuthority()));
	}

	private String resolveUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return String.valueOf(user.getId());
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot verify student data access");
	}
}
