package com.altafjava.school.application.scheduler;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily at 09:00 tenant-local time.
 * Sends fee payment reminders to students/parents whose fees are due within 3 days.
 */
@Slf4j
@Component
@ScheduledJob(name = "FeePaymentReminder", group = "school", description = "Reminds parents/students of upcoming fee due dates", cronExpression = "0 0 9 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class FeePaymentReminderJob implements JobExecutionStrategy {

	private final StudentRepository studentRepository;
	private final FeePaymentService feePaymentService;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;

	public FeePaymentReminderJob(StudentRepository studentRepository, FeePaymentService feePaymentService,
			StudentNotificationRecipientResolver recipientResolver, NotificationService notificationService) {
		this.studentRepository = studentRepository;
		this.feePaymentService = feePaymentService;
		this.recipientResolver = recipientResolver;
		this.notificationService = notificationService;
	}

	@Override
	public String jobName() {
		return "FeePaymentReminder";
	}

	@Override
	public String jobGroup() {
		return "school";
	}

	@Override
	public boolean isTenantScoped() {
		return true;
	}

	@Override
	public JobExecutionResult execute(JobExecutionContext ctx) {
		Long tenantId = TenantContext.getCurrentTenantId();
		log.info("action=fee-payment-reminder tenantId={} executionId={}", tenantId, ctx.executionId());

		List<Student> activeStudents = studentRepository.findAllByEnrollmentStatusAndTenantId(
				EnrollmentStatus.ACTIVE, tenantId);

		int remindedCount = 0;
		for (Student student : activeStudents) {
			BigDecimal outstanding = totalOutstanding(tenantId, student);
			if (outstanding.signum() <= 0) {
				continue;
			}
			if (notifyRecipient(tenantId, student, outstanding)) {
				remindedCount++;
			}
		}

		log.info("action=fee-payment-reminder-complete tenantId={} remindedCount={}", tenantId, remindedCount);
		return new JobExecutionResult.Success(Map.of("remindedCount", remindedCount), null);
	}

	private BigDecimal totalOutstanding(Long tenantId, Student student) {
		return feePaymentService.calculateBalanceForStudent(tenantId, student).stream()
				.map(FeeBalance::outstandingBalance)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private boolean notifyRecipient(Long tenantId, Student student, BigDecimal outstanding) {
		return recipientResolver.resolve(tenantId, student)
				.map(userId -> {
					String studentName = student.getFirstName() + " " + student.getLastName();
					notificationService.send(SendNotificationCommand.builder()
							.tenantId(tenantId)
							.userId(userId)
							.type(NotificationType.FEE_DUE)
							.title("Fee Payment Reminder")
							.message("Outstanding fee balance for " + studentName + " is " + outstanding
									+ ". Please pay at your earliest convenience.")
							.templateVariables(Map.of(
									"studentName", studentName,
									"amount", outstanding.toPlainString()))
							.priority(NotificationPriority.NORMAL)
							.build());
					return true;
				})
				.orElse(false);
	}
}
