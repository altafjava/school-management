package com.altafjava.school.api.controller;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.application.dashboard.AcademicDashboardDataProvider;
import com.altafjava.school.application.dashboard.FinanceDashboardDataProvider;
import com.altafjava.school.application.dashboard.HrDashboardDataProvider;
import com.altafjava.school.application.dashboard.PrincipalDashboardDataProvider;
import com.altafjava.school.application.security.SchoolRoles;

/**
 * Live JSON view of each role-scoped dashboard — calls the same {@code ReportDataProvider} bean
 * directly (synchronously) that {@code ServiceCallReportDataFetcher} also calls when a tenant
 * admin exports the identical data as a CSV/Excel/PDF report via the seeded {@code ReportDefinition}
 * (see {@code SchoolTenantProvisioningListener}) — one aggregation implementation, two consumption
 * paths, since a live dashboard wants an immediate JSON response, not the async
 * execute-then-poll-then-download flow the generic report pipeline is built for.
 */
@RestController
@RequestMapping("/api/v1/dashboards")
public class DashboardController {

	private final PrincipalDashboardDataProvider principalDashboardDataProvider;
	private final FinanceDashboardDataProvider financeDashboardDataProvider;
	private final HrDashboardDataProvider hrDashboardDataProvider;
	private final AcademicDashboardDataProvider academicDashboardDataProvider;

	public DashboardController(PrincipalDashboardDataProvider principalDashboardDataProvider,
			FinanceDashboardDataProvider financeDashboardDataProvider,
			HrDashboardDataProvider hrDashboardDataProvider,
			AcademicDashboardDataProvider academicDashboardDataProvider) {
		this.principalDashboardDataProvider = principalDashboardDataProvider;
		this.financeDashboardDataProvider = financeDashboardDataProvider;
		this.hrDashboardDataProvider = hrDashboardDataProvider;
		this.academicDashboardDataProvider = academicDashboardDataProvider;
	}

	@GetMapping("/principal")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_PRINCIPAL)
	public List<Map<String, Object>> principal() {
		return principalDashboardDataProvider.fetchData(Map.of());
	}

	@GetMapping("/finance")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_FINANCE)
	public List<Map<String, Object>> finance() {
		return financeDashboardDataProvider.fetchData(Map.of());
	}

	@GetMapping("/hr")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_HR)
	public List<Map<String, Object>> hr() {
		return hrDashboardDataProvider.fetchData(Map.of());
	}

	@GetMapping("/academic")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_ACADEMIC)
	public List<Map<String, Object>> academic() {
		return academicDashboardDataProvider.fetchData(Map.of());
	}
}
