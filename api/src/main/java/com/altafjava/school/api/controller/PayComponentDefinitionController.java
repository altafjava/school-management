package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.api.dto.request.CreatePayComponentDefinitionRequest;
import com.altafjava.school.api.dto.request.UpdatePayComponentDefinitionRequest;
import com.altafjava.school.api.dto.response.PayComponentDefinitionResponse;
import com.altafjava.school.api.mapper.PayComponentDefinitionMapper;
import com.altafjava.school.application.service.PayComponentDefinitionService;

@RestController
@RequestMapping("/api/v1/pay-component-definitions")
public class PayComponentDefinitionController {

	private final PayComponentDefinitionService payComponentDefinitionService;
	private final PayComponentDefinitionMapper payComponentDefinitionMapper;

	public PayComponentDefinitionController(PayComponentDefinitionService payComponentDefinitionService,
			PayComponentDefinitionMapper payComponentDefinitionMapper) {
		this.payComponentDefinitionService = payComponentDefinitionService;
		this.payComponentDefinitionMapper = payComponentDefinitionMapper;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PAY_COMPONENT_MANAGE')")
	public List<PayComponentDefinitionResponse> list() {
		return payComponentDefinitionService.list().stream().map(payComponentDefinitionMapper::toResponse).toList();
	}

	@GetMapping("/active")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PAY_COMPONENT_MANAGE')")
	public List<PayComponentDefinitionResponse> listActive() {
		return payComponentDefinitionService.listActive().stream().map(payComponentDefinitionMapper::toResponse)
				.toList();
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PAY_COMPONENT_MANAGE')")
	public PayComponentDefinitionResponse get(@PathVariable String publicId) {
		return payComponentDefinitionMapper.toResponse(payComponentDefinitionService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PAY_COMPONENT_MANAGE')")
	public PayComponentDefinitionResponse create(@Valid @RequestBody CreatePayComponentDefinitionRequest request) {
		return payComponentDefinitionMapper.toResponse(payComponentDefinitionService.create(request.code(),
				request.name(), request.type(), request.displayOrder()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PAY_COMPONENT_MANAGE')")
	public PayComponentDefinitionResponse update(@PathVariable String publicId,
			@Valid @RequestBody UpdatePayComponentDefinitionRequest request) {
		return payComponentDefinitionMapper.toResponse(payComponentDefinitionService.update(publicId, request.name(),
				request.active(), request.displayOrder()));
	}
}
