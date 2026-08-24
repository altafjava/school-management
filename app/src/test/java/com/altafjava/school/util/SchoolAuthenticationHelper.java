package com.altafjava.school.util;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.TokenProvider;

/**
 * Mints role-scoped JWTs directly for E2E tests, mirroring
 * {@code com.altafjava.platform.test.util.AuthenticationHelper} in platform-saas — that class is
 * test-scoped there and not published as a consumable artifact (tracked in
 * {@code school-saas/PLATFORM_GAPS.md}), so this is school-saas's own copy until a shared
 * {@code platform-test-support} artifact exists. Depends on {@link TokenProvider} (the
 * {@code application}-module interface {@code JwtService} implements), not the concrete
 * {@code infrastructure}-module class — school-saas never imports {@code platform.infrastructure}
 * directly, matching the rest of the codebase's module-boundary discipline.
 *
 * <p>
 * Used for "wrong role" 403 test cases where going through a full self-registration/invite flow
 * for a non-admin role would be disproportionate setup for what the assertion actually needs: a
 * token whose {@code roles} claim doesn't include the role a given endpoint requires.
 */
@Component
public class SchoolAuthenticationHelper {

	private final TokenProvider tokenProvider;

	public SchoolAuthenticationHelper(TokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	public String tokenWithRole(Long tenantId, String role) {
		// user_id must be present alongside a non-empty roles claim — JwtAuthenticationFilter's
		// buildUserDetails() only skips its real-user DB lookup (which a synthetic test principal
		// would fail) when both are set; without user_id it falls through to
		// userDetailsService.loadUserByUsername() and 401s before @PreAuthorize is ever evaluated.
		return tokenProvider.generateToken("test-" + role.toLowerCase() + "@school.test", tenantId,
				Map.of("roles", List.of(role), "user_id", -1L));
	}

	/**
	 * Mints a token carrying a real platform user's surrogate ID — needed for ownership-based
	 * RBAC tests (e.g. "a guardian can only view their own linked student's data") where
	 * {@link #tokenWithRole} synthetic {@code user_id = -1L} would fail
	 * {@code ResourceAccessPolicy}'s guardian/student lookup by design.
	 */
	public String tokenForUser(Long tenantId, Long userId, String email, String role) {
		return tokenProvider.generateToken(email, tenantId, Map.of("roles", List.of(role), "user_id", userId));
	}

	/**
	 * Mints a token for a user who holds an {@code OrganizationMembership} — carries both the
	 * membership's role (e.g. {@code ORG_ADMIN}) and the {@code org_id} claim
	 * {@code OrganizationAccessGuard}/{@code JwtAuthenticationFilter} read, alongside the user's
	 * normal home-tenant scope.
	 */
	public String tokenForOrgMember(Long tenantId, Long userId, String email, String orgRole, Long orgId) {
		return tokenProvider.generateToken(email, tenantId,
				Map.of("roles", List.of(orgRole), "user_id", userId, "org_id", orgId));
	}
}
