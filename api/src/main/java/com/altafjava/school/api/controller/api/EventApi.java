package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateEventRequest;
import com.altafjava.school.api.dto.request.RegisterForEventRequest;
import com.altafjava.school.api.dto.request.UpdateEventRequest;
import com.altafjava.school.api.dto.response.EventRegistrationResponse;
import com.altafjava.school.api.dto.response.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Event", description = "APIs for managing Event operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface EventApi {

	@Operation(summary = "List", operationId = "event_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<EventResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "event_get")
	public ApiResponse<EventResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", operationId = "event_create")
	public ApiResponse<EventResponse> create(@Valid @RequestBody CreateEventRequest request);

	@Operation(summary = "Update details", operationId = "event_updateDetails")
	public ApiResponse<EventResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateEventRequest request);

	@Operation(summary = "Cancel", operationId = "event_cancel")
	public ApiResponse<EventResponse> cancel(@PathVariable String publicId);

	@Operation(summary = "List registrations", operationId = "event_listRegistrations")
	public ApiResponse<com.altafjava.platform.core.model.Page<EventRegistrationResponse>> listRegistrations(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Register", operationId = "event_register")
	public ApiResponse<EventRegistrationResponse> register(@PathVariable String publicId,
			@Valid @RequestBody RegisterForEventRequest request);

	@Operation(summary = "Cancel registration", operationId = "event_cancelRegistration")
	public ApiResponse<EventRegistrationResponse> cancelRegistration(@PathVariable String registrationPublicId);

	@Operation(summary = "Mark attended", operationId = "event_markAttended")
	public ApiResponse<EventRegistrationResponse> markAttended(@PathVariable String registrationPublicId);
}
