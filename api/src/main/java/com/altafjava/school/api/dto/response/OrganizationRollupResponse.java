package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import java.util.List;

public record OrganizationRollupResponse(String organizationPublicId, String organizationName, LocalDate periodStart,
		LocalDate periodEnd, List<CampusRollupResponse> campuses, RollupTotalsResponse totals) {

	public OrganizationRollupResponse {
		campuses = List.copyOf(campuses);
	}
}
