package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertDispatchService;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.alert.ExamScheduleReminderRuleEvaluator;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily at 08:00 tenant-local time. Evaluates and dispatches the tenant's {@code
 * EXAM_SCHEDULE_REMINDER} alert rule (see {@link ExamScheduleReminderRuleEvaluator}).
 */
@Slf4j
@Component
@ScheduledJob(name = "ExamScheduleReminder", group = "school", description = "Notifies students and parents of upcoming exams", cronExpression = "0 0 8 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class ExamScheduleReminderJob implements JobExecutionStrategy {

	private final AlertDispatchService alertDispatchService;

	public ExamScheduleReminderJob(AlertDispatchService alertDispatchService) {
		this.alertDispatchService = alertDispatchService;
	}

	@Override
	public String jobName() {
		return "ExamScheduleReminder";
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
		log.info("action=exam-schedule-reminder tenantId={} executionId={}", tenantId, ctx.executionId());

		int remindedCount = alertDispatchService.evaluateAndDispatch(tenantId,
				ExamScheduleReminderRuleEvaluator.RULE_TYPE);

		log.info("action=exam-schedule-reminder-complete tenantId={} remindedCount={}", tenantId, remindedCount);
		return new JobExecutionResult.Success(Map.of("remindedCount", remindedCount), null);
	}
}
