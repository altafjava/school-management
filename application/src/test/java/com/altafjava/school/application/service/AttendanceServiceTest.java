package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
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
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
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
	private StudentDataAccessGuard studentDataAccessGuard;

	private AttendanceService attendanceService;

	@BeforeEach
	void setUp() {
		attendanceService = new AttendanceService(attendanceRepository, studentRepository, classroomRepository,
				studentDataAccessGuard);
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
	void mark_duplicateForSameStudentClassroomDate_throwsIllegalArgument() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
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
		when(attendanceRepository.existsByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(
				any(), any(), any(), any())).thenReturn(false);
		when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> attendanceService.mark(1L, 10L, LocalDate.now(), AttendanceStatus.PRESENT,
				"teacher"));
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
}
