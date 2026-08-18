package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class DailyAttendanceReminderJobTest {

	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private TeacherRepository teacherRepository;
	@Mock
	private NotificationService notificationService;

	private DailyAttendanceReminderJob job;

	@BeforeEach
	void setUp() {
		job = new DailyAttendanceReminderJob(classroomRepository, attendanceRepository, teacherRepository,
				notificationService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "DailyAttendanceReminder", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	private Classroom classroomWithTeacher(long id, Long teacherId) {
		Classroom classroom = Classroom.create("CLS-" + id, "Grade 5", "A", 1L, "2025-26", teacherId);
		classroom.setId(id);
		return classroom;
	}

	private Teacher teacherWithId(long id, Long userId) {
		Teacher teacher = Teacher.create("EMP-" + id, "Jane", "Doe", "jane@school.test", null);
		teacher.setId(id);
		teacher.setUserId(userId);
		return teacher;
	}

	@Test
	void execute_classroomWithoutTodaysAttendance_remindsTeacher() {
		Classroom classroom = classroomWithTeacher(10L, 20L);
		Teacher teacher = teacherWithId(20L, 99L);

		when(classroomRepository.findAllByTenantId(1L)).thenReturn(List.of(classroom));
		when(attendanceRepository.existsByClassroomIdAndAttendanceDateAndTenantId(10L, LocalDate.now(), 1L))
				.thenReturn(false);
		when(teacherRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.of(teacher));

		JobExecutionResult result = job.execute(context());

		ArgumentCaptor<SendNotificationCommand> captor = ArgumentCaptor.forClass(SendNotificationCommand.class);
		verify(notificationService).send(captor.capture());
		assertEquals(99L, captor.getValue().getUserId());
		assertEquals(NotificationType.ANNOUNCEMENT, captor.getValue().getType());
		assertEquals(new JobExecutionResult.Success(Map.of("remindedCount", 1), null), result);
	}

	@Test
	void execute_classroomWithTodaysAttendanceAlreadyMarked_sendsNoReminder() {
		Classroom classroom = classroomWithTeacher(10L, 20L);
		when(classroomRepository.findAllByTenantId(1L)).thenReturn(List.of(classroom));
		when(attendanceRepository.existsByClassroomIdAndAttendanceDateAndTenantId(10L, LocalDate.now(), 1L))
				.thenReturn(true);

		job.execute(context());

		verify(notificationService, never()).send(any());
	}

	@Test
	void execute_classroomWithNoTeacherAssigned_sendsNoReminder() {
		Classroom classroom = classroomWithTeacher(10L, null);
		when(classroomRepository.findAllByTenantId(1L)).thenReturn(List.of(classroom));

		job.execute(context());

		verify(notificationService, never()).send(any());
	}

	@Test
	void execute_teacherWithNoLoginAccount_sendsNoReminder() {
		Classroom classroom = classroomWithTeacher(10L, 20L);
		Teacher teacher = teacherWithId(20L, null);
		when(classroomRepository.findAllByTenantId(1L)).thenReturn(List.of(classroom));
		when(attendanceRepository.existsByClassroomIdAndAttendanceDateAndTenantId(10L, LocalDate.now(), 1L))
				.thenReturn(false);
		when(teacherRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.of(teacher));

		job.execute(context());

		verify(notificationService, never()).send(any());
	}
}
