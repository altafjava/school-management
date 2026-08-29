package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.SetCustomFieldValuesRequest;
import com.altafjava.school.api.dto.response.CustomFieldValueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Vehicle Custom Field", description = "APIs for managing Vehicle Custom Field operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface VehicleCustomFieldApi {

	@Operation(summary = "Get", operationId = "vehiclecustomfield_get")
	public ApiResponse<List<CustomFieldValueResponse>> get(@PathVariable String publicId);

	@Operation(summary = "Set", operationId = "vehiclecustomfield_set")
	public ApiResponse<List<CustomFieldValueResponse>> set(@PathVariable String publicId,
			@Valid @RequestBody SetCustomFieldValuesRequest request);
}
