package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeePaymentReminderJobTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private FeePaymentService feePaymentService;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private NotificationService notificationService;

	private FeePaymentReminderJob job;

	@BeforeEach
	void setUp() {
		job = new FeePaymentReminderJob(studentRepository, feePaymentService, recipientResolver,
				notificationService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "FeePaymentReminder", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void execute_studentWithOutstandingBalance_sendsFeeDueNotification() {
		Student student = studentWithId(1L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student));
		when(feePaymentService.calculateBalanceForStudent(1L, student))
				.thenReturn(List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(1000), BigDecimal.valueOf(400),
						BigDecimal.valueOf(600), BigDecimal.ZERO)));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(42L));

		JobExecutionResult result = job.execute(context());

		ArgumentCaptor<SendNotificationCommand> captor = ArgumentCaptor.forClass(SendNotificationCommand.class);
		verify(notificationService).send(captor.capture());
		SendNotificationCommand sent = captor.getValue();
		assertEquals(1L, sent.getTenantId());
		assertEquals(42L, sent.getUserId());
		assertEquals(NotificationType.FEE_DUE, sent.getType());
		assertTrue(sent.getMessage().contains("600"));
		assertTrue(sent.getMessage().contains("Alice"));
		assertEquals("Alice Smith", sent.getTemplateVariables().get("studentName"));
		assertEquals("600", sent.getTemplateVariables().get("amount"));

		assertEquals(new JobExecutionResult.Success(java.util.Map.of("remindedCount", 1), null), result);
	}

	@Test
	void execute_studentWithNoOutstandingBalance_sendsNoNotification() {
		Student student = studentWithId(1L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student));
		when(feePaymentService.calculateBalanceForStudent(1L, student))
				.thenReturn(List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000),
						BigDecimal.ZERO, BigDecimal.ZERO)));

		job.execute(context());

		verify(notificationService, never()).send(any());
	}

	@Test
	void execute_outstandingBalanceButNoResolvedRecipient_sendsNoNotification() {
		Student student = studentWithId(1L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student));
		when(feePaymentService.calculateBalanceForStudent(1L, student))
				.thenReturn(List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(1000), BigDecimal.ZERO,
						BigDecimal.valueOf(1000), BigDecimal.ZERO)));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.empty());

		JobExecutionResult result = job.execute(context());

		verify(notificationService, never()).send(any());
		assertEquals(new JobExecutionResult.Success(java.util.Map.of("remindedCount", 0), null), result);
	}

	@Test
	void execute_multipleStudentsWithBalance_notifiesEachOnce() {
		Student studentA = studentWithId(1L);
		Student studentB = studentWithId(2L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(studentA, studentB));
		when(feePaymentService.calculateBalanceForStudent(1L, studentA))
				.thenReturn(List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(500), BigDecimal.ZERO,
						BigDecimal.valueOf(500), BigDecimal.ZERO)));
		when(feePaymentService.calculateBalanceForStudent(1L, studentB))
				.thenReturn(List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(500), BigDecimal.ZERO,
						BigDecimal.valueOf(500), BigDecimal.ZERO)));
		when(recipientResolver.resolve(1L, studentA)).thenReturn(Optional.of(41L));
		when(recipientResolver.resolve(1L, studentB)).thenReturn(Optional.of(42L));

		job.execute(context());

		verify(notificationService, times(2)).send(any());
	}
}
