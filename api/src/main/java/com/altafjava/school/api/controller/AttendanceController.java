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
import com.altafjava.school.api.dto.request.MarkAttendanceRequest;
import com.altafjava.school.api.dto.request.UpdateAttendanceStatusRequest;
import com.altafjava.school.api.dto.response.AttendanceCorrectionResponse;
import com.altafjava.school.api.dto.response.AttendanceResponse;
import com.altafjava.school.api.mapper.AttendanceCorrectionMapper;
import com.altafjava.school.api.mapper.AttendanceMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.AttendanceService;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

	private final AttendanceService attendanceService;
	private final AttendanceMapper attendanceMapper;
	private final AttendanceCorrectionMapper attendanceCorrectionMapper;

	private final SpringDataPageableResolver pageableResolver;

	public AttendanceController(AttendanceService attendanceService, AttendanceMapper attendanceMapper,
			AttendanceCorrectionMapper attendanceCorrectionMapper, SpringDataPageableResolver pageableResolver) {
		this.attendanceService = attendanceService;
		this.attendanceMapper = attendanceMapper;
		this.attendanceCorrectionMapper = attendanceCorrectionMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_READ')")
	public Page<AttendanceResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return attendanceService.listAttendance(pageableResolver.resolve(page, size))
				.map(attendanceMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_READ')")
	public AttendanceResponse get(@PathVariable String publicId) {
		return attendanceMapper.toResponse(attendanceService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_WRITE')")
	public AttendanceResponse mark(@Valid @RequestBody MarkAttendanceRequest request) {
		return attendanceMapper.toResponse(attendanceService.mark(
				request.studentId(),
				request.classroomId(),
				request.attendanceDate(),
				request.status(),
				request.markedBy()));
	}

	@PatchMapping("/{publicId}/status")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_WRITE')")
	public AttendanceResponse updateStatus(@PathVariable String publicId,
			@Valid @RequestBody UpdateAttendanceStatusRequest request) {
		return attendanceMapper.toResponse(attendanceService.updateStatus(publicId, request.status()));
	}

	@GetMapping("/{publicId}/corrections")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_READ')")
	public Page<AttendanceCorrectionResponse> listCorrections(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return attendanceService.listCorrections(publicId, pageableResolver.resolve(page, size))
				.map(attendanceCorrectionMapper::toResponse);
	}

	@DeleteMapping("/{publicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_WRITE')")
	public void delete(@PathVariable String publicId) {
		attendanceService.delete(publicId);
	}
}
