package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import com.altafjava.platform.core.idempotency.RequireIdempotencyKey;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.dto.request.CheckoutBookRequest;
import com.altafjava.school.api.dto.request.ReturnBookRequest;
import com.altafjava.school.api.dto.response.CirculationResponse;
import com.altafjava.school.api.mapper.CirculationMapper;
import com.altafjava.school.application.service.CirculationService;

@RestController
@RequestMapping("/api/v1/circulations")
public class CirculationController {

	private final CirculationService circulationService;
	private final CirculationMapper circulationMapper;

	public CirculationController(CirculationService circulationService, CirculationMapper circulationMapper) {
		this.circulationService = circulationService;
		this.circulationMapper = circulationMapper;
	}

	@GetMapping
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public Page<CirculationResponse> listForStudent(
			@RequestParam String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return circulationService.listForStudent(studentPublicId, PageRequest.of(page, Math.min(size, 100)))
				.map(circulationMapper::toResponse);
	}

	@PostMapping("/checkout")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	@RequireIdempotencyKey
	public CirculationResponse checkout(@Valid @RequestBody CheckoutBookRequest request) {
		return circulationMapper.toResponse(
				circulationService.checkout(request.bookCopyPublicId(), request.studentPublicId()));
	}

	@PatchMapping("/{publicId}/return")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	@RequireIdempotencyKey
	public CirculationResponse returnBook(@PathVariable String publicId,
			@Valid @RequestBody ReturnBookRequest request) {
		return circulationMapper.toResponse(circulationService.returnBook(publicId, request.returnedAt()));
	}
}
