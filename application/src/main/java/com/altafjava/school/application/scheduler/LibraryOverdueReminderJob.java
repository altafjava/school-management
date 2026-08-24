package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertDispatchService;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.alert.LibraryOverdueRuleEvaluator;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs weekly at 08:00 tenant-local time on Mondays. Evaluates and dispatches the tenant's {@code
 * LIBRARY_BOOK_OVERDUE} alert rule (see {@link LibraryOverdueRuleEvaluator}).
 */
@Slf4j
@Component
@ScheduledJob(name = "LibraryOverdueReminder", group = "school", description = "Reminds students/guardians of overdue library books", cronExpression = "0 0 8 ? * MON", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class LibraryOverdueReminderJob implements JobExecutionStrategy {

	private final AlertDispatchService alertDispatchService;

	public LibraryOverdueReminderJob(AlertDispatchService alertDispatchService) {
		this.alertDispatchService = alertDispatchService;
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
	public JobExecutionResult execute(JobExecutionContext ctx) {
		Long tenantId = TenantContext.getCurrentTenantId();
		log.info("action=library-overdue-reminder tenantId={} executionId={}", tenantId, ctx.executionId());

		int remindedCount = alertDispatchService.evaluateAndDispatch(tenantId, LibraryOverdueRuleEvaluator.RULE_TYPE);

		log.info("action=library-overdue-reminder-complete tenantId={} remindedCount={}", tenantId, remindedCount);
		return new JobExecutionResult.Success(Map.of("remindedCount", remindedCount), null);
	}
}
