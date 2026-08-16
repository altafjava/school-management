package com.altafjava.school.api.controller;

import java.util.List;
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
import com.altafjava.school.api.dto.request.CreateClassroomRequest;
import com.altafjava.school.api.dto.response.ClassroomResponse;
import com.altafjava.school.api.dto.response.TimetableEntryResponse;
import com.altafjava.school.api.mapper.ClassroomMapper;
import com.altafjava.school.api.mapper.TimetableEntryMapper;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.TimetableService;

@RestController
@RequestMapping("/api/v1/classrooms")
public class ClassroomController {

	private final ClassroomService classroomService;
	private final ClassroomMapper classroomMapper;
	private final TimetableService timetableService;
	private final TimetableEntryMapper timetableEntryMapper;

	public ClassroomController(ClassroomService classroomService, ClassroomMapper classroomMapper,
			TimetableService timetableService, TimetableEntryMapper timetableEntryMapper) {
		this.classroomService = classroomService;
		this.classroomMapper = classroomMapper;
		this.timetableService = timetableService;
		this.timetableEntryMapper = timetableEntryMapper;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<ClassroomResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return classroomService.listClassrooms(PageRequest.of(page, Math.min(size, 100)))
				.map(classroomMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public ClassroomResponse get(@PathVariable String publicId) {
		return classroomMapper.toResponse(classroomService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public ClassroomResponse create(@Valid @RequestBody CreateClassroomRequest request) {
		return classroomMapper.toResponse(classroomService.create(
				request.classCode(),
				request.grade(),
				request.section(),
				request.academicYear(),
				request.classTeacherId()));
	}

	@GetMapping("/{publicId}/timetable")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public List<TimetableEntryResponse> timetable(@PathVariable String publicId) {
		return timetableEntryMapper.toResponseList(timetableService.listForClassroom(publicId));
	}
}
