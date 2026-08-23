package com.altafjava.school.application.scheduler;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
import com.altafjava.school.domain.library.model.Circulation;
import com.altafjava.school.domain.library.repository.CirculationRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;

/** Weekly reminder for every open (not-yet-returned) circulation past its due date. */
@Slf4j
@Component
@ScheduledJob(name = "LibraryOverdueReminder", group = "school", description = "Reminds students/guardians of overdue library books", cronExpression = "0 0 8 ? * MON", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class LibraryOverdueReminderJob implements JobExecutionStrategy {

	private final CirculationRepository circulationRepository;
	private final StudentRepository studentRepository;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;

	public LibraryOverdueReminderJob(CirculationRepository circulationRepository, StudentRepository studentRepository,
			StudentNotificationRecipientResolver recipientResolver, NotificationService notificationService) {
		this.circulationRepository = circulationRepository;
		this.studentRepository = studentRepository;
		this.recipientResolver = recipientResolver;
		this.notificationService = notificationService;
	}

	@Override
	public String jobName() {
		return "LibraryOverdueReminder";
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
	@Transactional
	public JobExecutionResult execute(JobExecutionContext ctx) {
		Long tenantId = TenantContext.getCurrentTenantId();
		LocalDate today = LocalDate.now();
		log.info("action=library-overdue-reminder tenantId={} executionId={}", tenantId, ctx.executionId());

		int remindedCount = 0;
		for (Circulation circulation : circulationRepository.findAllByTenantIdAndReturnedAtIsNull(tenantId)) {
			if (circulation.isOverdue(today) && remind(tenantId, circulation)) {
				remindedCount++;
			}
		}

		log.info("action=library-overdue-reminder-complete tenantId={} remindedCount={}", tenantId, remindedCount);
		return new JobExecutionResult.Success(Map.of("remindedCount", remindedCount), null);
	}

	private boolean remind(Long tenantId, Circulation circulation) {
		Optional<Student> student = studentRepository.findByIdAndTenantId(circulation.getStudentId(), tenantId);
		if (student.isEmpty()) {
			return false;
		}
		Optional<Long> recipientUserId = recipientResolver.resolve(tenantId, student.get());
		if (recipientUserId.isEmpty()) {
			return false;
		}
		notificationService.send(SendNotificationCommand.builder()
				.tenantId(tenantId)
				.userId(recipientUserId.get())
				.type(NotificationType.BOOK_OVERDUE)
				.title("Overdue Library Book")
				.message("A library book is overdue since " + circulation.getDueDate())
				.templateVariables(Map.of(
						"studentName", student.get().getFirstName() + " " + student.get().getLastName(),
						"dueDate", circulation.getDueDate().toString()))
				.priority(NotificationPriority.NORMAL)
				.build());
		return true;
	}
}
