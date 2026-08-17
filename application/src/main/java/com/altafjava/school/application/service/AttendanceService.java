package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.application.security.TeacherClassroomScopeResolver;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class AttendanceService {

	private final AttendanceRepository attendanceRepository;
	private final StudentRepository studentRepository;
	private final ClassroomRepository classroomRepository;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final TeacherClassroomScopeResolver teacherClassroomScopeResolver;

	public AttendanceService(AttendanceRepository attendanceRepository, StudentRepository studentRepository,
			ClassroomRepository classroomRepository, StudentDataAccessGuard studentDataAccessGuard,
			TeacherClassroomScopeResolver teacherClassroomScopeResolver) {
		this.attendanceRepository = attendanceRepository;
		this.studentRepository = studentRepository;
		this.classroomRepository = classroomRepository;
		this.studentDataAccessGuard = studentDataAccessGuard;
		this.teacherClassroomScopeResolver = teacherClassroomScopeResolver;
	}

	// TENANT_ADMIN sees every attendance record; TEACHER sees only records for classrooms they
	// teach (resolved via TeacherClassroomScopeResolver — see ROADMAP.md Phase 3).
	@Transactional(readOnly = true)
	public Page<Attendance> listAttendance(Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return teacherClassroomScopeResolver.resolveClassroomIdsIfTeacherScoped(tenantId)
				.map(classroomIds -> attendanceRepository.findByClassroomIdInAndTenantId(classroomIds, tenantId,
						pageable))
				.orElseGet(() -> attendanceRepository.findAllByTenantId(tenantId, pageable));
	}

	@Transactional(readOnly = true)
	public Attendance findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return attendanceRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + publicId));
	}

	@Transactional(readOnly = true)
	public Page<Attendance> getStudentAttendance(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return attendanceRepository.findByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional
	public Attendance mark(Long studentId, Long classroomId, LocalDate attendanceDate,
			AttendanceStatus status, String markedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!studentRepository.existsByIdAndTenantId(studentId, tenantId)) {
			throw new ResourceNotFoundException("Student not found: " + studentId);
		}
		if (!classroomRepository.existsByIdAndTenantId(classroomId, tenantId)) {
			throw new ResourceNotFoundException("Classroom not found: " + classroomId);
		}
		if (attendanceRepository.existsByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(
				studentId, classroomId, attendanceDate, tenantId)) {
			throw new IllegalArgumentException(
					"Attendance already marked for student " + studentId + " on " + attendanceDate);
		}
		Attendance attendance = Attendance.create(studentId, classroomId, attendanceDate, status, markedBy);
		return attendanceRepository.save(attendance);
	}
}
