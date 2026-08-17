package com.altafjava.school.api.controller;

import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.dto.response.OrganizationRollupResponse;
import com.altafjava.school.api.mapper.OrganizationRollupMapper;
import com.altafjava.school.application.rollup.OrganizationRollupService;

/**
 * Organization-level rollup reporting — aggregates attendance, fees, and enrollment across every
 * campus in a school group (see ROADMAP.md Phase 4). {@code SUPER_ADMIN}-only: {@link
 * OrganizationRollupService}'s cross-campus reads are only correct when the request resolves to
 * the platform's system tenant (no Hibernate {@code tenantFilter} bound for the whole request) —
 * see that class's Javadoc. Matches platform's own {@code OrganizationController} gating; a
 * campus-scoped {@code TENANT_ADMIN}/{@code ORG_ADMIN}-level rollup view is platform work not yet
 * built (no bridge from a tenant-scoped {@code User} to {@code OrganizationMembership} exists
 * today) and is out of scope for this phase.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationPublicId}/rollup-report")
@PreAuthorize(Roles.HAS_SUPER_ADMIN)
public class OrganizationRollupController {

	private final OrganizationRollupService organizationRollupService;
	private final OrganizationRollupMapper organizationRollupMapper;

	public OrganizationRollupController(OrganizationRollupService organizationRollupService,
			OrganizationRollupMapper organizationRollupMapper) {
		this.organizationRollupService = organizationRollupService;
		this.organizationRollupMapper = organizationRollupMapper;
	}

	@GetMapping
	public OrganizationRollupResponse get(
			@PathVariable String organizationPublicId,
			@RequestParam LocalDate from,
			@RequestParam LocalDate to) {
		return organizationRollupMapper.toResponse(
				organizationRollupService.generate(organizationPublicId, from, to));
	}
}
