package com.altafjava.school.api.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.dto.request.CreateStudentRequest;
import com.altafjava.school.api.dto.request.UpdateStudentContactDetailsRequest;
import com.altafjava.school.api.dto.response.AttendancePercentageResponse;
import com.altafjava.school.api.dto.response.AttendanceResponse;
import com.altafjava.school.api.dto.response.BulkImportResponse;
import com.altafjava.school.api.dto.response.FeeBalanceResponse;
import com.altafjava.school.api.dto.response.GpaResponse;
import com.altafjava.school.api.dto.response.GradeResponse;
import com.altafjava.school.api.dto.response.ReportCardResponse;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.api.mapper.AttendanceMapper;
import com.altafjava.school.api.mapper.AttendancePercentageMapper;
import com.altafjava.school.api.mapper.BulkImportMapper;
import com.altafjava.school.api.mapper.FeeBalanceMapper;
import com.altafjava.school.api.mapper.GradeMapper;
import com.altafjava.school.api.mapper.ReportCardMapper;
import com.altafjava.school.api.mapper.StudentMapper;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.AttendanceService;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.service.GpaResult;
import com.altafjava.school.application.service.GradeService;
import com.altafjava.school.application.service.ReportCardService;
import com.altafjava.school.application.service.StudentBulkImportService;
import com.altafjava.school.application.service.StudentGpaService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.TermService;
import com.altafjava.school.domain.reportcard.model.ReportCard;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
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
	private final AttendancePercentageMapper attendancePercentageMapper;
	private final FeePaymentService feePaymentService;
	private final FeeBalanceMapper feeBalanceMapper;
	private final ReportCardService reportCardService;
	private final ReportCardMapper reportCardMapper;
	private final TermService termService;
	private final StudentBulkImportService studentBulkImportService;
	private final BulkImportMapper bulkImportMapper;
	private final StudentGpaService studentGpaService;

	public StudentController(StudentService studentService, StudentMapper studentMapper, GradeService gradeService,
			GradeMapper gradeMapper, AttendanceService attendanceService, AttendanceMapper attendanceMapper,
			AttendancePercentageMapper attendancePercentageMapper, FeePaymentService feePaymentService,
			FeeBalanceMapper feeBalanceMapper, ReportCardService reportCardService, ReportCardMapper reportCardMapper,
			TermService termService, StudentBulkImportService studentBulkImportService,
			BulkImportMapper bulkImportMapper, StudentGpaService studentGpaService) {
		this.studentService = studentService;
		this.studentMapper = studentMapper;
		this.gradeService = gradeService;
		this.gradeMapper = gradeMapper;
		this.attendanceService = attendanceService;
		this.attendanceMapper = attendanceMapper;
		this.attendancePercentageMapper = attendancePercentageMapper;
		this.feePaymentService = feePaymentService;
		this.feeBalanceMapper = feeBalanceMapper;
		this.reportCardService = reportCardService;
		this.reportCardMapper = reportCardMapper;
		this.termService = termService;
		this.studentBulkImportService = studentBulkImportService;
		this.bulkImportMapper = bulkImportMapper;
		this.studentGpaService = studentGpaService;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<StudentResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) EnrollmentStatus status) {
		return studentService.listStudents(PageRequest.of(page, Math.min(size, 100)), status)
				.map(studentMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public StudentResponse get(@PathVariable String publicId) {
		return studentMapper.toResponse(studentService.findByPublicId(publicId));
	}

	@PostMapping("/bulk-import")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public BulkImportResponse bulkImport(@RequestParam("file") MultipartFile file) {
		try (var inputStream = file.getInputStream()) {
			return bulkImportMapper.toResponse(studentBulkImportService.importCsv(inputStream));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read uploaded CSV file", e);
		}
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

	@PatchMapping("/{publicId}/withdraw")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public StudentResponse withdraw(@PathVariable String publicId) {
		return studentMapper.toResponse(studentService.withdraw(publicId));
	}

	@PatchMapping("/{publicId}/graduate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public StudentResponse graduate(@PathVariable String publicId) {
		return studentMapper.toResponse(studentService.graduate(publicId));
	}

	@PatchMapping("/{publicId}/contact-details")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public StudentResponse updateContactDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateStudentContactDetailsRequest request) {
		return studentMapper.toResponse(studentService.updateContactDetails(publicId, request.firstName(),
				request.lastName(), request.email(), request.dateOfBirth()));
	}

	@GetMapping("/{publicId}/grades")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public Page<GradeResponse> grades(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return gradeService.getStudentGrades(publicId, PageRequest.of(page, Math.min(size, 100)))
				.map(gradeMapper::toResponse);
	}

	@GetMapping("/{publicId}/gpa/term")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public GpaResponse termGpa(@PathVariable String publicId, @RequestParam String termPublicId) {
		return toGpaResponse(studentGpaService.calculateTermGpa(publicId, termPublicId));
	}

	@GetMapping("/{publicId}/gpa/academic-year")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public GpaResponse academicYearGpa(@PathVariable String publicId, @RequestParam String academicYearPublicId) {
		return toGpaResponse(studentGpaService.calculateAcademicYearGpa(publicId, academicYearPublicId));
	}

	@GetMapping("/{publicId}/gpa/cumulative")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public GpaResponse cumulativeGpa(@PathVariable String publicId) {
		return toGpaResponse(studentGpaService.calculateCumulativeGpa(publicId));
	}

	private GpaResponse toGpaResponse(GpaResult result) {
		return new GpaResponse(result.gpa(), result.gradeCount());
	}

	@GetMapping("/{publicId}/attendance")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public Page<AttendanceResponse> attendance(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return attendanceService.getStudentAttendance(publicId, PageRequest.of(page, Math.min(size, 100)))
				.map(attendanceMapper::toResponse);
	}

	@GetMapping("/{publicId}/attendance/percentage")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public AttendancePercentageResponse attendancePercentage(@PathVariable String publicId,
			@RequestParam LocalDate fromDate,
			@RequestParam LocalDate toDate) {
		return attendancePercentageMapper.toResponse(
				attendanceService.calculatePercentage(publicId, fromDate, toDate));
	}

	@GetMapping("/{publicId}/fee-balance")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_PARENT_OR_STUDENT)
	public List<FeeBalanceResponse> feeBalance(@PathVariable String publicId) {
		return feeBalanceMapper.toResponseList(feePaymentService.calculateBalance(publicId));
	}

	@GetMapping("/{publicId}/report-cards")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public Page<ReportCardResponse> reportCards(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return reportCardService.listForStudent(publicId, PageRequest.of(page, Math.min(size, 100)))
				.map(reportCardMapper::toResponse);
	}

	@PostMapping("/{publicId}/report-cards")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public ReportCardResponse generateReportCard(@PathVariable String publicId,
			@RequestParam String termPublicId) {
		Student student = studentService.findByPublicId(publicId);
		var term = termService.findByPublicId(termPublicId);
		ReportCard reportCard = reportCardService.generate(student.getId(), term.getId());
		return reportCardMapper.toResponse(reportCard);
	}

	@GetMapping("/{publicId}/report-cards/{reportCardPublicId}/download")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public ResponseEntity<byte[]> downloadReportCard(@PathVariable String publicId,
			@PathVariable String reportCardPublicId) {
		ReportCard reportCard = reportCardService.findByPublicId(publicId, reportCardPublicId);
		byte[] pdf = reportCardService.downloadPdf(reportCard);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(reportCardPublicId + ".pdf").build().toString())
				.body(pdf);
	}
}
