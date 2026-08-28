package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.altafjava.school.api.dto.request.CreateHolidayRequest;
import com.altafjava.school.api.dto.request.UpdateHolidayRequest;
import com.altafjava.school.api.dto.response.HolidayResponse;
import com.altafjava.school.api.mapper.HolidayMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.HolidayService;

// The tenant's school-calendar holiday list — feeds attendance-percentage and leave-day
// calculations (see HolidayService#datesInRange), so read access is broad but writes are
// tenant-admin-only.
@RestController
@RequestMapping("/api/v1/holidays")
public class HolidayController {

	private final HolidayService holidayService;
	private final HolidayMapper holidayMapper;

	private final SpringDataPageableResolver pageableResolver;

	public HolidayController(HolidayService holidayService, HolidayMapper holidayMapper,
			SpringDataPageableResolver pageableResolver) {
		this.holidayService = holidayService;
		this.holidayMapper = holidayMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<HolidayResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return holidayService.list(pageableResolver.resolve(page, size)).map(holidayMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public HolidayResponse get(@PathVariable String publicId) {
		return holidayMapper.toResponse(holidayService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public HolidayResponse create(@Valid @RequestBody CreateHolidayRequest request) {
		return holidayMapper.toResponse(holidayService.create(request.date(), request.name(), request.recurring()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public HolidayResponse updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateHolidayRequest request) {
		return holidayMapper.toResponse(
				holidayService.updateDetails(publicId, request.date(), request.name(), request.recurring()));
	}

	@DeleteMapping("/{publicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public void delete(@PathVariable String publicId) {
		holidayService.delete(publicId);
	}
}
