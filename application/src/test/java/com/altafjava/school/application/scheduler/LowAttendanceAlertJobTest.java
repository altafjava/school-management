package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.platform.domain.scheduler.model.TriggerType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.service.AttendanceAlertSettingsService;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class LowAttendanceAlertJobTest {

	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private AttendanceAlertSettingsService attendanceAlertSettingsService;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private NotificationService notificationService;

	private LowAttendanceAlertJob job;

	@BeforeEach
	void setUp() {
		job = new LowAttendanceAlertJob(studentClassroomLinkRepository, attendanceRepository, studentRepository,
				attendanceAlertSettingsService, recipientResolver, notificationService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "LowAttendanceAlert", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void execute_studentBelowThreshold_sendsLowAttendanceAlert() {
		when(attendanceAlertSettingsService.getLowThresholdPercent()).thenReturn(75);
		when(studentClassroomLinkRepository.findDistinctStudentIdsByTenantId(1L)).thenReturn(List.of(1L));
		when(attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetween(eq(1L), eq(1L),
				any(LocalDate.class), any(LocalDate.class))).thenReturn(20L);
		when(attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatus(eq(1L), eq(1L),
				any(LocalDate.class), any(LocalDate.class), eq(AttendanceStatus.PRESENT))).thenReturn(10L);
		Student student = studentWithId(1L);
		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(77L));

		JobExecutionResult result = job.execute(context());

		ArgumentCaptor<SendNotificationCommand> captor = ArgumentCaptor.forClass(SendNotificationCommand.class);
		verify(notificationService).send(captor.capture());
		SendNotificationCommand sent = captor.getValue();
		assertEquals(77L, sent.getUserId());
		assertEquals(NotificationType.LOW_ATTENDANCE_ALERT, sent.getType());
		assertEquals("Alice Smith", sent.getTemplateVariables().get("studentName"));
		assertEquals("50.00", sent.getTemplateVariables().get("percentage"));
		assertEquals("30", sent.getTemplateVariables().get("windowDays"));
		assertEquals(new JobExecutionResult.Success(Map.of("alertedCount", 1), null), result);
	}

	@Test
	void execute_studentAtOrAboveThreshold_sendsNoAlert() {
		when(attendanceAlertSettingsService.getLowThresholdPercent()).thenReturn(75);
		when(studentClassroomLinkRepository.findDistinctStudentIdsByTenantId(1L)).thenReturn(List.of(1L));
		when(attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetween(eq(1L), eq(1L),
				any(LocalDate.class), any(LocalDate.class))).thenReturn(20L);
		when(attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatus(eq(1L), eq(1L),
				any(LocalDate.class), any(LocalDate.class), eq(AttendanceStatus.PRESENT))).thenReturn(16L);

		JobExecutionResult result = job.execute(context());

		verify(notificationService, never()).send(any());
		assertEquals(new JobExecutionResult.Success(Map.of("alertedCount", 0), null), result);
	}

	@Test
	void execute_studentWithNoMarkedAttendance_skipsWithoutError() {
		when(attendanceAlertSettingsService.getLowThresholdPercent()).thenReturn(75);
		when(studentClassroomLinkRepository.findDistinctStudentIdsByTenantId(1L)).thenReturn(List.of(1L));
		when(attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetween(eq(1L), eq(1L),
				any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);

		JobExecutionResult result = job.execute(context());

		verify(notificationService, never()).send(any());
		verify(attendanceRepository, never()).countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatus(any(),
				any(), any(), any(), any());
		assertEquals(new JobExecutionResult.Success(Map.of("alertedCount", 0), null), result);
	}

	@Test
	void execute_noRosterStudents_sendsNoAlerts() {
		when(attendanceAlertSettingsService.getLowThresholdPercent()).thenReturn(75);
		when(studentClassroomLinkRepository.findDistinctStudentIdsByTenantId(1L)).thenReturn(List.of());

		JobExecutionResult result = job.execute(context());

		verify(notificationService, never()).send(any());
		assertEquals(new JobExecutionResult.Success(Map.of("alertedCount", 0), null), result);
	}
}
