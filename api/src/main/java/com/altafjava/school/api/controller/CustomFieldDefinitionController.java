package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.dto.request.CreateCustomFieldDefinitionRequest;
import com.altafjava.school.api.dto.request.UpdateCustomFieldDefinitionRequest;
import com.altafjava.school.api.dto.response.CustomFieldDefinitionResponse;
import com.altafjava.school.api.mapper.CustomFieldDefinitionMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.CustomFieldDefinitionService;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;

// Tenant-admin-only: this is the "schema" a tenant admin defines for their dynamic/custom fields.
@RestController
@RequestMapping("/api/v1/custom-field-definitions")
public class CustomFieldDefinitionController {

	private final CustomFieldDefinitionService customFieldDefinitionService;
	private final CustomFieldDefinitionMapper customFieldDefinitionMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CustomFieldDefinitionController(CustomFieldDefinitionService customFieldDefinitionService,
			CustomFieldDefinitionMapper customFieldDefinitionMapper, SpringDataPageableResolver pageableResolver) {
		this.customFieldDefinitionService = customFieldDefinitionService;
		this.customFieldDefinitionMapper = customFieldDefinitionMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<CustomFieldDefinitionResponse> list(
			@RequestParam(required = false) CustomFieldEntityType entityType,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return customFieldDefinitionService.list(entityType, pageableResolver.resolve(page, size))
				.map(customFieldDefinitionMapper::toResponse);
	}

	@GetMapping("/active")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public List<CustomFieldDefinitionResponse> listActive(@RequestParam CustomFieldEntityType entityType) {
		return customFieldDefinitionService.listActive(entityType).stream()
				.map(customFieldDefinitionMapper::toResponse)
				.toList();
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public CustomFieldDefinitionResponse get(@PathVariable String publicId) {
		return customFieldDefinitionMapper.toResponse(customFieldDefinitionService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public CustomFieldDefinitionResponse create(@Valid @RequestBody CreateCustomFieldDefinitionRequest request) {
		return customFieldDefinitionMapper.toResponse(customFieldDefinitionService.create(request.entityType(),
				request.fieldKey(), request.label(), request.fieldType(), request.required()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public CustomFieldDefinitionResponse updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateCustomFieldDefinitionRequest request) {
		return customFieldDefinitionMapper.toResponse(customFieldDefinitionService.updateDetails(publicId,
				request.label(), request.fieldType(), request.required()));
	}

	@PatchMapping("/{publicId}/activate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public CustomFieldDefinitionResponse activate(@PathVariable String publicId) {
		return customFieldDefinitionMapper.toResponse(customFieldDefinitionService.activate(publicId));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public CustomFieldDefinitionResponse deactivate(@PathVariable String publicId) {
		return customFieldDefinitionMapper.toResponse(customFieldDefinitionService.deactivate(publicId));
	}
}
