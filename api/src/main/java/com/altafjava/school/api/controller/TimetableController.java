package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.dto.request.CreateTimetableEntryRequest;
import com.altafjava.school.api.dto.response.TimetableEntryResponse;
import com.altafjava.school.api.mapper.TimetableEntryMapper;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.TimetableService;

@RestController
@RequestMapping("/api/v1/timetable-entries")
public class TimetableController {

	private final TimetableService timetableService;
	private final TimetableEntryMapper timetableEntryMapper;

	public TimetableController(TimetableService timetableService, TimetableEntryMapper timetableEntryMapper) {
		this.timetableService = timetableService;
		this.timetableEntryMapper = timetableEntryMapper;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<TimetableEntryResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return timetableService.listEntries(PageRequest.of(page, Math.min(size, 100)))
				.map(timetableEntryMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public TimetableEntryResponse get(@PathVariable String publicId) {
		return timetableEntryMapper.toResponse(timetableService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public TimetableEntryResponse schedule(@Valid @RequestBody CreateTimetableEntryRequest request) {
		return timetableEntryMapper.toResponse(timetableService.schedule(
				request.dayOfWeek(),
				request.periodId(),
				request.classroomId(),
				request.subjectId(),
				request.teacherId()));
	}
}
