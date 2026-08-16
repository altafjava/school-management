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
import com.altafjava.school.api.dto.request.CreateStudentRequest;
import com.altafjava.school.api.dto.response.AttendanceResponse;
import com.altafjava.school.api.dto.response.FeeBalanceResponse;
import com.altafjava.school.api.dto.response.GradeResponse;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.api.mapper.AttendanceMapper;
import com.altafjava.school.api.mapper.FeeBalanceMapper;
import com.altafjava.school.api.mapper.GradeMapper;
import com.altafjava.school.api.mapper.StudentMapper;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.AttendanceService;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.service.GradeService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.domain.student.model.Student;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

	private final StudentService studentService;
	private final StudentMapper studentMapper;
	private final GradeService gradeService;
	private final GradeMapper gradeMapper;
	private final AttendanceService attendanceService;
	private final AttendanceMapper attendanceMapper;
	private final FeePaymentService feePaymentService;
	private final FeeBalanceMapper feeBalanceMapper;

	public StudentController(StudentService studentService, StudentMapper studentMapper, GradeService gradeService,
			GradeMapper gradeMapper, AttendanceService attendanceService, AttendanceMapper attendanceMapper,
			FeePaymentService feePaymentService, FeeBalanceMapper feeBalanceMapper) {
		this.studentService = studentService;
		this.studentMapper = studentMapper;
		this.gradeService = gradeService;
		this.gradeMapper = gradeMapper;
		this.attendanceService = attendanceService;
		this.attendanceMapper = attendanceMapper;
		this.feePaymentService = feePaymentService;
		this.feeBalanceMapper = feeBalanceMapper;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<StudentResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return studentService.listStudents(PageRequest.of(page, Math.min(size, 100)))
				.map(studentMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public StudentResponse get(@PathVariable String publicId) {
		return studentMapper.toResponse(studentService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public StudentResponse enroll(@Valid @RequestBody CreateStudentRequest request) {
		Student student = studentService.enroll(
				request.studentCode(),
				request.firstName(),
				request.lastName(),
				request.email(),
				request.dateOfBirth());
		return studentMapper.toResponse(student);
	}

	@GetMapping("/{publicId}/grades")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public Page<GradeResponse> grades(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return gradeService.getStudentGrades(publicId, PageRequest.of(page, Math.min(size, 100)))
				.map(gradeMapper::toResponse);
	}

	@GetMapping("/{publicId}/attendance")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public Page<AttendanceResponse> attendance(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return attendanceService.getStudentAttendance(publicId, PageRequest.of(page, Math.min(size, 100)))
				.map(attendanceMapper::toResponse);
	}

	@GetMapping("/{publicId}/fee-balance")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_PARENT_OR_STUDENT)
	public List<FeeBalanceResponse> feeBalance(@PathVariable String publicId) {
		return feeBalanceMapper.toResponseList(feePaymentService.calculateBalance(publicId));
	}
}
