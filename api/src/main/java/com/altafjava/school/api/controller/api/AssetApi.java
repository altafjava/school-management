package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateAssetRequest;
import com.altafjava.school.api.dto.request.UpdateAssetLocationRequest;
import com.altafjava.school.api.dto.response.AssetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Asset", description = "APIs for managing Asset operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AssetApi {

	@Operation(summary = "List", operationId = "asset_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<AssetResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "asset_get")
	public ApiResponse<AssetResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", operationId = "asset_create")
	public ApiResponse<AssetResponse> create(@Valid @RequestBody CreateAssetRequest request);

	@Operation(summary = "Update location", operationId = "asset_updateLocation")
	public ApiResponse<AssetResponse> updateLocation(@PathVariable String publicId,
			@Valid @RequestBody UpdateAssetLocationRequest request);

	@Operation(summary = "Mark under maintenance", operationId = "asset_markUnderMaintenance")
	public ApiResponse<AssetResponse> markUnderMaintenance(@PathVariable String publicId);

	@Operation(summary = "Mark available", operationId = "asset_markAvailable")
	public ApiResponse<AssetResponse> markAvailable(@PathVariable String publicId);

	@Operation(summary = "Mark disposed", operationId = "asset_markDisposed")
	public ApiResponse<AssetResponse> markDisposed(@PathVariable String publicId);
}
