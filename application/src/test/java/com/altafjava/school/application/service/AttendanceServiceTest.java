package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.application.security.TeacherClassroomScopeResolver;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;
	@Mock
	private TeacherClassroomScopeResolver teacherClassroomScopeResolver;

	private AttendanceService attendanceService;

	@BeforeEach
	void setUp() {
		attendanceService = new AttendanceService(attendanceRepository, studentRepository, classroomRepository,
				studentClassroomLinkRepository, studentDataAccessGuard, teacherClassroomScopeResolver);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void mark_withNonExistentStudent_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.mark(99L, 10L, LocalDate.now(), AttendanceStatus.PRESENT, "teacher"));

		verify(attendanceRepository, never()).save(any());
	}

	@Test
	void mark_withNonExistentClassroom_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.mark(1L, 99L, LocalDate.now(), AttendanceStatus.PRESENT, "teacher"));

		verify(attendanceRepository, never()).save(any());
	}

	@Test
	void mark_studentNotEnrolledInClassroom_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.mark(1L, 10L, LocalDate.now(), AttendanceStatus.PRESENT, "teacher"));

		verify(attendanceRepository, never()).save(any());
	}

	@Test
	void mark_duplicateForSameStudentClassroomDate_throwsIllegalArgument() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L))
				.thenReturn(Optional.of(StudentClassroomLink.create(1L, 10L, 5L, LocalDate.now())));
		LocalDate date = LocalDate.now();
		when(attendanceRepository.existsByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(1L, 10L, date, 1L))
				.thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> attendanceService.mark(1L, 10L, date, AttendanceStatus.PRESENT, "teacher"));
	}

	@Test
	void mark_withValidReferences_succeeds() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(studentClassroomLinkRepository.findByStudentIdAndClassroomId(1L, 1L, 10L))
				.thenReturn(Optional.of(StudentClassroomLink.create(1L, 10L, 5L, LocalDate.now())));
		when(attendanceRepository.existsByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(
				any(), any(), any(), any())).thenReturn(false);
		when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> attendanceService.mark(1L, 10L, LocalDate.now(), AttendanceStatus.PRESENT,
				"teacher"));
	}

	@Test
	void listAttendance_asTenantAdmin_returnsAllTenantAttendance() {
		when(teacherClassroomScopeResolver.resolveClassroomIdsIfTeacherScoped(1L))
				.thenReturn(java.util.Optional.empty());
		var expected = org.springframework.data.domain.Page.<Attendance>empty();
		when(attendanceRepository.findAllByTenantId(1L, org.springframework.data.domain.PageRequest.of(0, 20)))
				.thenReturn(expected);

		attendanceService.listAttendance(org.springframework.data.domain.PageRequest.of(0, 20));

		verify(attendanceRepository).findAllByTenantId(1L, org.springframework.data.domain.PageRequest.of(0, 20));
		verify(attendanceRepository, never()).findByClassroomIdInAndTenantId(any(), any(), any());
	}

	@Test
	void listAttendance_asScopedTeacher_filtersByTheirClassroomIds() {
		when(teacherClassroomScopeResolver.resolveClassroomIdsIfTeacherScoped(1L))
				.thenReturn(java.util.Optional.of(java.util.List.of(10L, 11L)));
		var expected = org.springframework.data.domain.Page.<Attendance>empty();
		when(attendanceRepository.findByClassroomIdInAndTenantId(java.util.List.of(10L, 11L), 1L,
				org.springframework.data.domain.PageRequest.of(0, 20))).thenReturn(expected);

		attendanceService.listAttendance(org.springframework.data.domain.PageRequest.of(0, 20));

		verify(attendanceRepository).findByClassroomIdInAndTenantId(java.util.List.of(10L, 11L), 1L,
				org.springframework.data.domain.PageRequest.of(0, 20));
		verify(attendanceRepository, never()).findAllByTenantId(any(), any());
	}

	@Test
	void getStudentAttendance_delegatesToAccessGuard() {
		var student = com.altafjava.school.domain.student.model.Student.create(
				"STU-1", "Alice", "Smith", "alice@school.test", null);
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(java.util.Optional.of(student));
		org.mockito.Mockito.doNothing().when(studentDataAccessGuard).assertCanView(any(), any());

		assertDoesNotThrow(() -> attendanceService.getStudentAttendance(
				"11111111-1111-1111-1111-111111111111", org.springframework.data.domain.PageRequest.of(0, 20)));

		verify(studentDataAccessGuard).assertCanView(1L, "11111111-1111-1111-1111-111111111111");
	}

	@Test
	void calculatePercentage_delegatesToAccessGuardAndCalculator() {
		var student = com.altafjava.school.domain.student.model.Student.create(
				"STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(42L);
		String studentPublicId = "11111111-1111-1111-1111-111111111111";
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate to = LocalDate.of(2026, 1, 31);
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(student));
		org.mockito.Mockito.doNothing().when(studentDataAccessGuard).assertCanView(any(), any());
		when(attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetween(42L, 1L, from, to))
				.thenReturn(20L);
		when(attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatus(42L, 1L, from, to,
				AttendanceStatus.PRESENT)).thenReturn(18L);

		var result = attendanceService.calculatePercentage(studentPublicId, from, to);

		verify(studentDataAccessGuard).assertCanView(1L, studentPublicId);
		org.junit.jupiter.api.Assertions.assertEquals(18L, result.presentDays());
		org.junit.jupiter.api.Assertions.assertEquals(20L, result.totalMarkedDays());
		org.junit.jupiter.api.Assertions.assertEquals(0,
				java.math.BigDecimal.valueOf(90.00).compareTo(result.percentage()));
	}

	@Test
	void calculatePercentage_withNonExistentStudent_throwsResourceNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> attendanceService.calculatePercentage("11111111-1111-1111-1111-111111111111",
						LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));

		verify(studentDataAccessGuard, never()).assertCanView(any(), any());
	}
}
