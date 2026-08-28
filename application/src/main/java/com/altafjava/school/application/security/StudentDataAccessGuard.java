package com.altafjava.school.application.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.platform.application.security.ResourceAccessPolicyEnforcer;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.application.policy.ResourceAction;
import com.altafjava.school.application.policy.ResourceType;

// Enforces "own child/own record only" on student-scoped reads; anyone holding STUDENT_READ
// (TENANT_ADMIN always, TEACHER by default seed, or any tenant-granted custom role) bypasses it.
@Component
public class StudentDataAccessGuard {

	private final ResourceAccessPolicyEnforcer resourceAccessPolicyEnforcer;
	private final PermissionAuthorizationService permissionAuthorizationService;

	public StudentDataAccessGuard(ResourceAccessPolicyEnforcer resourceAccessPolicyEnforcer,
			PermissionAuthorizationService permissionAuthorizationService) {
		this.resourceAccessPolicyEnforcer = resourceAccessPolicyEnforcer;
		this.permissionAuthorizationService = permissionAuthorizationService;
	}

	public void assertCanView(Long tenantId, String studentPublicId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (permissionAuthorizationService.hasPermission("STUDENT_READ")) {
			return;
		}
		String userId = resolveUserId(authentication);
		boolean allowed = resourceAccessPolicyEnforcer.isAllowed(userId, tenantId, ResourceType.STUDENT.name(),
				studentPublicId, ResourceAction.READ.name());
		if (!allowed) {
			throw new AccessDeniedException("Not authorized to view student " + studentPublicId + "'s data");
		}
	}

	private String resolveUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return String.valueOf(user.getId());
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot verify student data access");
	}
}
