package com.altafjava.school.api.controller;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.DashboardApi;
import com.altafjava.school.application.dashboard.AcademicDashboardDataProvider;
import com.altafjava.school.application.dashboard.AttendanceTrendDataProvider;
import com.altafjava.school.application.dashboard.FeeCollectionTrendDataProvider;
import com.altafjava.school.application.dashboard.FinanceDashboardDataProvider;
import com.altafjava.school.application.dashboard.HrDashboardDataProvider;
import com.altafjava.school.application.dashboard.LeaveUtilizationTrendDataProvider;
import com.altafjava.school.application.dashboard.PrincipalDashboardDataProvider;

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
public class DashboardController implements DashboardApi {

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

	@Override
	@GetMapping("/principal")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_PRINCIPAL_READ')")
	public ApiResponse<List<Map<String, Object>>> principal() {
		return ApiResponse.success(principalDashboardDataProvider.fetchData(Map.of()));
	}

	@Override
	@GetMapping("/finance")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_FINANCE_READ')")
	public ApiResponse<List<Map<String, Object>>> finance() {
		return ApiResponse.success(financeDashboardDataProvider.fetchData(Map.of()));
	}

	@Override
	@GetMapping("/hr")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_HR_READ')")
	public ApiResponse<List<Map<String, Object>>> hr() {
		return ApiResponse.success(hrDashboardDataProvider.fetchData(Map.of()));
	}

	@Override
	@GetMapping("/academic")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_ACADEMIC_READ')")
	public ApiResponse<List<Map<String, Object>>> academic() {
		return ApiResponse.success(academicDashboardDataProvider.fetchData(Map.of()));
	}

	@Override
	@GetMapping("/principal/trends")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_PRINCIPAL_READ')")
	public ApiResponse<List<Map<String, Object>>> principalTrends(@RequestParam(required = false) Integer periods) {
		return ApiResponse.success(attendanceTrendDataProvider.fetchData(trendParameters(periods)));
	}

	@Override
	@GetMapping("/academic/trends")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_ACADEMIC_READ')")
	public ApiResponse<List<Map<String, Object>>> academicTrends(@RequestParam(required = false) Integer periods) {
		return ApiResponse.success(attendanceTrendDataProvider.fetchData(trendParameters(periods)));
	}

	@Override
	@GetMapping("/finance/trends")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_FINANCE_READ')")
	public ApiResponse<List<Map<String, Object>>> financeTrends(@RequestParam(required = false) Integer periods) {
		return ApiResponse.success(feeCollectionTrendDataProvider.fetchData(trendParameters(periods)));
	}

	@Override
	@GetMapping("/hr/trends")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_HR_READ')")
	public ApiResponse<List<Map<String, Object>>> hrTrends(@RequestParam(required = false) Integer periods) {
		return ApiResponse.success(leaveUtilizationTrendDataProvider.fetchData(trendParameters(periods)));
	}

	private Map<String, Object> trendParameters(Integer periods) {
		return periods == null ? Map.of() : Map.of("periods", periods);
	}
}
