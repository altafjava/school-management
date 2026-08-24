package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertDispatchService;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.alert.AttendanceNotMarkedRuleEvaluator;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily at 07:00 tenant-local time. Evaluates and dispatches the tenant's {@code
 * DAILY_ATTENDANCE_NOT_MARKED} alert rule (see {@link AttendanceNotMarkedRuleEvaluator}) —
 * reminds class teachers, not students/guardians.
 */
@Slf4j
@Component("schoolDailyAttendanceReminderJob")
@ScheduledJob(name = "DailyAttendanceReminder", group = "school", description = "Reminds teachers to mark attendance", cronExpression = "0 0 7 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class DailyAttendanceReminderJob implements JobExecutionStrategy {

	private final AlertDispatchService alertDispatchService;

	public DailyAttendanceReminderJob(AlertDispatchService alertDispatchService) {
		this.alertDispatchService = alertDispatchService;
	}

	@Override
	public String jobName() {
		return "DailyAttendanceReminder";
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
		log.info("action=attendance-reminder tenantId={} executionId={}", tenantId, ctx.executionId());

		int remindedCount = alertDispatchService.evaluateAndDispatch(tenantId,
				AttendanceNotMarkedRuleEvaluator.RULE_TYPE);

		log.info("action=attendance-reminder-complete tenantId={} remindedCount={}", tenantId, remindedCount);
		return new JobExecutionResult.Success(Map.of("remindedCount", remindedCount), null);
	}
}
