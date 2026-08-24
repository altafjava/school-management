package com.altafjava.school.api.controller;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.application.dashboard.AcademicDashboardDataProvider;
import com.altafjava.school.application.dashboard.AttendanceTrendDataProvider;
import com.altafjava.school.application.dashboard.FeeCollectionTrendDataProvider;
import com.altafjava.school.application.dashboard.FinanceDashboardDataProvider;
import com.altafjava.school.application.dashboard.HrDashboardDataProvider;
import com.altafjava.school.application.dashboard.LeaveUtilizationTrendDataProvider;
import com.altafjava.school.application.dashboard.PrincipalDashboardDataProvider;
import com.altafjava.school.application.security.SchoolRoles;

/**
 * Live JSON view of each role-scoped dashboard — calls the same {@code ReportDataProvider} bean
 * directly (synchronously) that {@code ServiceCallReportDataFetcher} also calls when a tenant
 * admin exports the identical data as a CSV/Excel/PDF report via the seeded {@code ReportDefinition}
 * (see {@code SchoolTenantProvisioningListener}) — one aggregation implementation, two consumption
 * paths, since a live dashboard wants an immediate JSON response, not the async
 * execute-then-poll-then-download flow the generic report pipeline is built for.
 *
 * <p>
 * {@code /trends} endpoints (Phase 4) are the same pattern applied to historical, multi-row data
 * instead of a single current-state summary row — {@code periods} controls how many trailing
 * weeks/months to return (defaults live on each provider).
 */
@RestController
@RequestMapping("/api/v1/dashboards")
public class DashboardController {

	private final PrincipalDashboardDataProvider principalDashboardDataProvider;
	private final FinanceDashboardDataProvider financeDashboardDataProvider;
	private final HrDashboardDataProvider hrDashboardDataProvider;
	private final AcademicDashboardDataProvider academicDashboardDataProvider;
	private final AttendanceTrendDataProvider attendanceTrendDataProvider;
	private final FeeCollectionTrendDataProvider feeCollectionTrendDataProvider;
	private final LeaveUtilizationTrendDataProvider leaveUtilizationTrendDataProvider;

	public DashboardController(PrincipalDashboardDataProvider principalDashboardDataProvider,
			FinanceDashboardDataProvider financeDashboardDataProvider,
			HrDashboardDataProvider hrDashboardDataProvider,
			AcademicDashboardDataProvider academicDashboardDataProvider,
			AttendanceTrendDataProvider attendanceTrendDataProvider,
			FeeCollectionTrendDataProvider feeCollectionTrendDataProvider,
			LeaveUtilizationTrendDataProvider leaveUtilizationTrendDataProvider) {
		this.principalDashboardDataProvider = principalDashboardDataProvider;
		this.financeDashboardDataProvider = financeDashboardDataProvider;
		this.hrDashboardDataProvider = hrDashboardDataProvider;
		this.academicDashboardDataProvider = academicDashboardDataProvider;
		this.attendanceTrendDataProvider = attendanceTrendDataProvider;
		this.feeCollectionTrendDataProvider = feeCollectionTrendDataProvider;
		this.leaveUtilizationTrendDataProvider = leaveUtilizationTrendDataProvider;
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

	@GetMapping("/principal/trends")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_PRINCIPAL)
	public List<Map<String, Object>> principalTrends(@RequestParam(required = false) Integer periods) {
		return attendanceTrendDataProvider.fetchData(trendParameters(periods));
	}

	@GetMapping("/academic/trends")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_ACADEMIC)
	public List<Map<String, Object>> academicTrends(@RequestParam(required = false) Integer periods) {
		return attendanceTrendDataProvider.fetchData(trendParameters(periods));
	}

	@GetMapping("/finance/trends")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_FINANCE)
	public List<Map<String, Object>> financeTrends(@RequestParam(required = false) Integer periods) {
		return feeCollectionTrendDataProvider.fetchData(trendParameters(periods));
	}

	@GetMapping("/hr/trends")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_HR)
	public List<Map<String, Object>> hrTrends(@RequestParam(required = false) Integer periods) {
		return leaveUtilizationTrendDataProvider.fetchData(trendParameters(periods));
	}

	private Map<String, Object> trendParameters(Integer periods) {
		return periods == null ? Map.of() : Map.of("periods", periods);
	}
}
