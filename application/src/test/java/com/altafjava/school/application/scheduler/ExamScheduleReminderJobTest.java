package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
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
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;

@ExtendWith(MockitoExtension.class)
class ExamScheduleReminderJobTest {

	@Mock
	private ExamRepository examRepository;
	@Mock
	private SubjectRepository subjectRepository;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private NotificationService notificationService;

	private ExamScheduleReminderJob job;

	@BeforeEach
	void setUp() {
		job = new ExamScheduleReminderJob(examRepository, subjectRepository, attendanceRepository, studentRepository,
				recipientResolver, notificationService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "ExamScheduleReminder", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	private Exam examWithId(long id, long classroomId, long subjectId) {
		Exam exam = Exam.create("Midterm", subjectId, classroomId, LocalDateTime.now().plusDays(1),
				BigDecimal.valueOf(100), null);
		exam.setId(id);
		return exam;
	}

	private Subject subjectWithId(long id, String name) {
		Subject subject = Subject.create("SUB-" + id, name, null);
		subject.setId(id);
		return subject;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void execute_studentInExamClassroom_sendsExamScheduledNotification() {
		Exam exam = examWithId(100L, 10L, 5L);
		Subject subject = subjectWithId(5L, "Math");
		Student student = studentWithId(1L);

		when(examRepository.findUpcoming(org.mockito.ArgumentMatchers.eq(1L), any(), any()))
				.thenReturn(List.of(exam));
		when(subjectRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(subject));
		when(attendanceRepository.findDistinctStudentIdsByClassroomId(1L, 10L)).thenReturn(List.of(1L));
		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(77L));

		JobExecutionResult result = job.execute(context());

		ArgumentCaptor<SendNotificationCommand> captor = ArgumentCaptor.forClass(SendNotificationCommand.class);
		verify(notificationService).send(captor.capture());
		SendNotificationCommand sent = captor.getValue();
		assertEquals(77L, sent.getUserId());
		assertEquals(NotificationType.EXAM_SCHEDULED, sent.getType());
		assertEquals(new JobExecutionResult.Success(Map.of("remindedCount", 1), null), result);
	}

	@Test
	void execute_examWithMissingSubject_skipsWithoutError() {
		Exam exam = examWithId(100L, 10L, 5L);
		when(examRepository.findUpcoming(org.mockito.ArgumentMatchers.eq(1L), any(), any()))
				.thenReturn(List.of(exam));
		when(subjectRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.empty());

		job.execute(context());

		verify(notificationService, never()).send(any());
		verify(attendanceRepository, never()).findDistinctStudentIdsByClassroomId(any(), any());
	}

	@Test
	void execute_noUpcomingExams_sendsNoNotifications() {
		when(examRepository.findUpcoming(org.mockito.ArgumentMatchers.eq(1L), any(), any()))
				.thenReturn(List.of());

		JobExecutionResult result = job.execute(context());

		verify(notificationService, never()).send(any());
		assertEquals(new JobExecutionResult.Success(Map.of("remindedCount", 0), null), result);
	}
}
