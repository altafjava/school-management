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
import com.altafjava.school.api.dto.request.AssignClassroomCurriculumRequest;
import com.altafjava.school.api.dto.request.CreateClassroomRequest;
import com.altafjava.school.api.dto.request.EnrollStudentInClassroomRequest;
import com.altafjava.school.api.dto.request.MoveClassroomAcademicYearRequest;
import com.altafjava.school.api.dto.request.ReassignClassTeacherRequest;
import com.altafjava.school.api.dto.request.UpdateClassroomCapacityRequest;
import com.altafjava.school.api.dto.response.ClassroomResponse;
import com.altafjava.school.api.dto.response.StudentClassroomLinkResponse;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.api.dto.response.TimetableEntryResponse;
import com.altafjava.school.api.mapper.ClassroomMapper;
import com.altafjava.school.api.mapper.StudentClassroomLinkMapper;
import com.altafjava.school.api.mapper.StudentMapper;
import com.altafjava.school.api.mapper.TimetableEntryMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.TimetableService;

@RestController
@RequestMapping("/api/v1/classrooms")
public class ClassroomController {

	private final ClassroomService classroomService;
	private final ClassroomMapper classroomMapper;
	private final TimetableService timetableService;
	private final TimetableEntryMapper timetableEntryMapper;
	private final StudentClassroomLinkMapper studentClassroomLinkMapper;
	private final StudentMapper studentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public ClassroomController(ClassroomService classroomService, ClassroomMapper classroomMapper,
			TimetableService timetableService, TimetableEntryMapper timetableEntryMapper,
			StudentClassroomLinkMapper studentClassroomLinkMapper, StudentMapper studentMapper,
			SpringDataPageableResolver pageableResolver) {
		this.classroomService = classroomService;
		this.classroomMapper = classroomMapper;
		this.timetableService = timetableService;
		this.timetableEntryMapper = timetableEntryMapper;
		this.studentClassroomLinkMapper = studentClassroomLinkMapper;
		this.studentMapper = studentMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_READ')")
	public Page<ClassroomResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return classroomService.listClassrooms(pageableResolver.resolve(page, size))
				.map(classroomMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_READ')")
	public ClassroomResponse get(@PathVariable String publicId) {
		return classroomMapper.toResponse(classroomService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ClassroomResponse create(@Valid @RequestBody CreateClassroomRequest request) {
		return classroomMapper.toResponse(classroomService.create(
				request.classCode(),
				request.grade(),
				request.section(),
				request.academicYearPublicId(),
				request.classTeacherId()));
	}

	@PatchMapping("/{publicId}/teacher")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ClassroomResponse reassignTeacher(@PathVariable String publicId,
			@Valid @RequestBody ReassignClassTeacherRequest request) {
		return classroomMapper.toResponse(classroomService.reassignTeacher(publicId, request.teacherId()));
	}

	@PatchMapping("/{publicId}/academic-year")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ClassroomResponse moveToAcademicYear(@PathVariable String publicId,
			@Valid @RequestBody MoveClassroomAcademicYearRequest request) {
		return classroomMapper.toResponse(
				classroomService.moveToAcademicYear(publicId, request.academicYearPublicId()));
	}

	@PatchMapping("/{publicId}/capacity")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ClassroomResponse updateCapacity(@PathVariable String publicId,
			@Valid @RequestBody UpdateClassroomCapacityRequest request) {
		return classroomMapper.toResponse(classroomService.updateCapacity(publicId, request.capacity()));
	}

	@PatchMapping("/{publicId}/curriculum")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ClassroomResponse assignCurriculum(@PathVariable String publicId,
			@Valid @RequestBody AssignClassroomCurriculumRequest request) {
		return classroomMapper.toResponse(classroomService.assignCurriculum(publicId, request.curriculumPublicId()));
	}

	@GetMapping("/{publicId}/timetable")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_READ')")
	public List<TimetableEntryResponse> timetable(@PathVariable String publicId) {
		return timetableEntryMapper.toResponseList(timetableService.listForClassroom(publicId));
	}

	@PostMapping("/{publicId}/students")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public StudentClassroomLinkResponse enrollStudent(@PathVariable String publicId,
			@Valid @RequestBody EnrollStudentInClassroomRequest request) {
		return studentClassroomLinkMapper.toResponse(
				classroomService.enrollStudent(publicId, request.studentPublicId(), request.academicYearPublicId()),
				request.studentPublicId(), publicId, request.academicYearPublicId());
	}

	@GetMapping("/{publicId}/students")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_READ')")
	public Page<StudentResponse> roster(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return classroomService.listRoster(publicId, pageableResolver.resolve(page, size))
				.map(studentMapper::toResponse);
	}

	@PatchMapping("/{publicId}/students/{studentPublicId}/withdraw")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public void withdrawStudent(@PathVariable String publicId, @PathVariable String studentPublicId) {
		classroomService.withdrawStudentFromClassroom(publicId, studentPublicId);
	}
}
