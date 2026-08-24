package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertDispatchService;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.alert.FeePaymentReminderRuleEvaluator;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily at 09:00 tenant-local time. Evaluates and dispatches the tenant's {@code
 * FEE_PAYMENT_REMINDER} alert rule (see {@link FeePaymentReminderRuleEvaluator}).
 */
@Slf4j
@Component
@ScheduledJob(name = "FeePaymentReminder", group = "school", description = "Reminds parents/students of outstanding fee balances", cronExpression = "0 0 9 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class FeePaymentReminderJob implements JobExecutionStrategy {

	private final AlertDispatchService alertDispatchService;

	public FeePaymentReminderJob(AlertDispatchService alertDispatchService) {
		this.alertDispatchService = alertDispatchService;
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

		int remindedCount = alertDispatchService.evaluateAndDispatch(tenantId,
				FeePaymentReminderRuleEvaluator.RULE_TYPE);

		log.info("action=fee-payment-reminder-complete tenantId={} remindedCount={}", tenantId, remindedCount);
		return new JobExecutionResult.Success(Map.of("remindedCount", remindedCount), null);
	}
}
