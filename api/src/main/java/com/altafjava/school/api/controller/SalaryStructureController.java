package com.altafjava.school.api.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import com.altafjava.school.api.dto.request.CreateSalaryStructureRequest;
import com.altafjava.school.api.dto.request.PayComponentAmountRequest;
import com.altafjava.school.api.dto.request.SupersedeSalaryStructureRequest;
import com.altafjava.school.api.dto.response.SalaryStructureResponse;
import com.altafjava.school.api.mapper.SalaryStructureMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.SalaryStructureService;

@RestController
@RequestMapping("/api/v1/salary-structures")
public class SalaryStructureController {

	private final SalaryStructureService salaryStructureService;
	private final SalaryStructureMapper salaryStructureMapper;

	private final SpringDataPageableResolver pageableResolver;

	public SalaryStructureController(SalaryStructureService salaryStructureService,
			SalaryStructureMapper salaryStructureMapper, SpringDataPageableResolver pageableResolver) {
		this.salaryStructureService = salaryStructureService;
		this.salaryStructureMapper = salaryStructureMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SALARY_STRUCTURE_MANAGE')")
	public Page<SalaryStructureResponse> listForTeacher(
			@RequestParam String teacherPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return salaryStructureService.listForTeacher(teacherPublicId, pageableResolver.resolve(page, size))
				.map(salaryStructureMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SALARY_STRUCTURE_MANAGE')")
	public SalaryStructureResponse get(@PathVariable String publicId) {
		return salaryStructureMapper.toResponse(salaryStructureService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SALARY_STRUCTURE_MANAGE')")
	public SalaryStructureResponse create(@Valid @RequestBody CreateSalaryStructureRequest request) {
		return salaryStructureMapper.toResponse(salaryStructureService.create(request.teacherPublicId(),
				toAmountsByCode(request.components()), request.effectiveFrom()));
	}

	// Narrow PATCH: the current active structure is superseded by a new one (never edited in
	// place), mirroring SalaryStructureService's one-active-per-teacher invariant.
	@PatchMapping("/{publicId}/supersede")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SALARY_STRUCTURE_MANAGE')")
	public SalaryStructureResponse supersede(@PathVariable String publicId,
			@Valid @RequestBody SupersedeSalaryStructureRequest request) {
		return salaryStructureMapper.toResponse(salaryStructureService.supersede(publicId,
				toAmountsByCode(request.components()), request.effectiveFrom()));
	}

	// LinkedHashMap explicitly: preserves the caller's component ordering through to persistence
	// and the response, rather than Collectors.toMap's unspecified (HashMap) iteration order.
	private Map<String, BigDecimal> toAmountsByCode(List<PayComponentAmountRequest> components) {
		return components.stream()
				.collect(Collectors.toMap(PayComponentAmountRequest::code, PayComponentAmountRequest::amount,
						(a, b) -> a, java.util.LinkedHashMap::new));
	}
}
