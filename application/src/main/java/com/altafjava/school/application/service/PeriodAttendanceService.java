package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.model.PeriodAttendance;
import com.altafjava.school.domain.attendance.repository.PeriodAttendanceRepository;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;

@Service
public class PeriodAttendanceService {

	private final PeriodAttendanceRepository periodAttendanceRepository;
	private final StudentRepository studentRepository;
	private final ClassroomRepository classroomRepository;
	private final TimetableEntryRepository timetableEntryRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;

	public PeriodAttendanceService(PeriodAttendanceRepository periodAttendanceRepository,
			StudentRepository studentRepository, ClassroomRepository classroomRepository,
			TimetableEntryRepository timetableEntryRepository,
			StudentClassroomLinkRepository studentClassroomLinkRepository) {
		this.periodAttendanceRepository = periodAttendanceRepository;
		this.studentRepository = studentRepository;
		this.classroomRepository = classroomRepository;
		this.timetableEntryRepository = timetableEntryRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
	}

	@Transactional(readOnly = true)
	public Page<PeriodAttendance> listAttendance(Pageable pageable) {
		return periodAttendanceRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public PeriodAttendance findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return periodAttendanceRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Period attendance record not found: " + publicId));
	}

	@Transactional(readOnly = true)
	public Page<PeriodAttendance> getStudentAttendance(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		var student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		return periodAttendanceRepository.findByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional
	public PeriodAttendance mark(Long studentId, Long classroomId, Long timetableEntryId, LocalDate attendanceDate,
			AttendanceStatus status, String markedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!studentRepository.existsByIdAndTenantId(studentId, tenantId)) {
			throw new ResourceNotFoundException("Student not found: " + studentId);
		}
		if (!classroomRepository.existsByIdAndTenantId(classroomId, tenantId)) {
			throw new ResourceNotFoundException("Classroom not found: " + classroomId);
		}
		TimetableEntry timetableEntry = timetableEntryRepository.findByIdAndTenantId(timetableEntryId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found: " + timetableEntryId));
		if (!timetableEntry.getClassroomId().equals(classroomId)) {
			throw new BusinessException(
					"Timetable entry " + timetableEntryId + " does not belong to classroom " + classroomId);
		}
		if (studentClassroomLinkRepository.findByStudentIdAndClassroomId(tenantId, studentId, classroomId)
				.isEmpty()) {
			throw new ResourceNotFoundException(
					"Student " + studentId + " is not enrolled in classroom " + classroomId);
		}
		if (periodAttendanceRepository.existsByStudentIdAndTimetableEntryIdAndAttendanceDateAndTenantId(studentId,
				timetableEntryId, attendanceDate, tenantId)) {
			throw new IllegalArgumentException("Period attendance already marked for student " + studentId
					+ " for timetable entry " + timetableEntryId + " on " + attendanceDate);
		}
		PeriodAttendance attendance = PeriodAttendance.create(studentId, classroomId, timetableEntryId,
				attendanceDate, status, markedBy);
		return periodAttendanceRepository.save(attendance);
	}
}
