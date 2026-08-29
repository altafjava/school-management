package com.altafjava.school.api.controller.api;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Dashboard", description = "APIs for managing Dashboard operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface DashboardApi {

	@Operation(summary = "Principal", operationId = "dashboard_principal")
	public ApiResponse<List<Map<String, Object>>> principal();

	@Operation(summary = "Finance", operationId = "dashboard_finance")
	public ApiResponse<List<Map<String, Object>>> finance();

	@Operation(summary = "Hr", operationId = "dashboard_hr")
	public ApiResponse<List<Map<String, Object>>> hr();

	@Operation(summary = "Academic", operationId = "dashboard_academic")
	public ApiResponse<List<Map<String, Object>>> academic();

	@Operation(summary = "Principal trends", operationId = "dashboard_principalTrends")
	public ApiResponse<List<Map<String, Object>>> principalTrends(@RequestParam(required = false) Integer periods);

	@Operation(summary = "Academic trends", operationId = "dashboard_academicTrends")
	public ApiResponse<List<Map<String, Object>>> academicTrends(@RequestParam(required = false) Integer periods);

	@Operation(summary = "Finance trends", operationId = "dashboard_financeTrends")
	public ApiResponse<List<Map<String, Object>>> financeTrends(@RequestParam(required = false) Integer periods);

	@Operation(summary = "Hr trends", operationId = "dashboard_hrTrends")
	public ApiResponse<List<Map<String, Object>>> hrTrends(@RequestParam(required = false) Integer periods);
}
