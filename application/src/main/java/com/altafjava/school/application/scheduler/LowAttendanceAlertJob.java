package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertDispatchService;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.alert.LowAttendanceRuleEvaluator;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs weekly at 07:00 tenant-local time on Mondays. Evaluates and dispatches the tenant's {@code
 * LOW_ATTENDANCE} alert rule (see {@link LowAttendanceRuleEvaluator}) — enable/disable and
 * threshold are tenant-admin-configurable via {@code AlertRuleController}, not code.
 */
@Slf4j
@Component
@ScheduledJob(name = "LowAttendanceAlert", group = "school", description = "Notifies parents/students when trailing 30-day attendance falls below the tenant's threshold", cronExpression = "0 0 7 ? * MON", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class LowAttendanceAlertJob implements JobExecutionStrategy {

	private final AlertDispatchService alertDispatchService;

	public LowAttendanceAlertJob(AlertDispatchService alertDispatchService) {
		this.alertDispatchService = alertDispatchService;
	}

	@Override
	public String jobName() {
		return "LowAttendanceAlert";
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
		log.info("action=low-attendance-alert tenantId={} executionId={}", tenantId, ctx.executionId());

		int alertedCount = alertDispatchService.evaluateAndDispatch(tenantId, LowAttendanceRuleEvaluator.RULE_TYPE);

		log.info("action=low-attendance-alert-complete tenantId={} alertedCount={}", tenantId, alertedCount);
		return new JobExecutionResult.Success(Map.of("alertedCount", alertedCount), null);
	}
}
