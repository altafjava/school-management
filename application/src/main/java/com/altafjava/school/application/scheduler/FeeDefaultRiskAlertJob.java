package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertDispatchService;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.alert.FeeDefaultRiskRuleEvaluator;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily at 09:00 tenant-local time. Evaluates and dispatches the tenant's {@code
 * FEE_DEFAULT_RISK} alert rule (see {@link FeeDefaultRiskRuleEvaluator}) — a staff-facing
 * default-risk signal, new in Phase 4, distinct from {@link FeePaymentReminderJob}'s parent-facing
 * reminder.
 */
@Slf4j
@Component
@ScheduledJob(name = "FeeDefaultRiskAlert", group = "school", description = "Alerts finance/tenant-admin staff of students whose outstanding balance exceeds the default-risk threshold", cronExpression = "0 0 9 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class FeeDefaultRiskAlertJob implements JobExecutionStrategy {

	private final AlertDispatchService alertDispatchService;

	public FeeDefaultRiskAlertJob(AlertDispatchService alertDispatchService) {
		this.alertDispatchService = alertDispatchService;
	}

	@Override
	public String jobName() {
		return "FeeDefaultRiskAlert";
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
		log.info("action=fee-default-risk-alert tenantId={} executionId={}", tenantId, ctx.executionId());

		int alertedCount = alertDispatchService.evaluateAndDispatch(tenantId, FeeDefaultRiskRuleEvaluator.RULE_TYPE);

		log.info("action=fee-default-risk-alert-complete tenantId={} alertedCount={}", tenantId, alertedCount);
		return new JobExecutionResult.Success(Map.of("alertedCount", alertedCount), null);
	}
}
