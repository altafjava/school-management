package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.scheduler.support.TenantAdminNotifier;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Triggered at end of term (configurable per tenant via job data map).
 * Identifies the students eligible for a report card and notifies tenant admins that batch
 * generation is due. PDF generation itself is a separate, later capability (ROADMAP.md Phase 3)
 * — this job's job is the real, verifiable trigger signal, not the document.
 * Default schedule: last day of March and last day of October at 23:00.
 */
@Slf4j
@Component
@ScheduledJob(name = "ReportCardGeneration", group = "school", description = "Batch generates report cards for all students at end of term", cronExpression = "0 0 23 L 3,10 ?", tenantScoped = true, retryEnabled = true, maxRetries = 1)
public class ReportCardGenerationJob implements JobExecutionStrategy {

	private final StudentRepository studentRepository;
	private final TenantAdminNotifier tenantAdminNotifier;

	public ReportCardGenerationJob(StudentRepository studentRepository, TenantAdminNotifier tenantAdminNotifier) {
		this.studentRepository = studentRepository;
		this.tenantAdminNotifier = tenantAdminNotifier;
	}

	@Override
	public String jobName() {
		return "ReportCardGeneration";
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
		log.info("action=report-card-generation tenantId={} executionId={}", tenantId, ctx.executionId());

		long eligibleCount = studentRepository.countByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, tenantId);

		int notifiedCount = 0;
		if (eligibleCount > 0) {
			String message = "Report card generation is due for " + eligibleCount + " active student(s).";
			notifiedCount = tenantAdminNotifier.notifyAll(tenantId, "Report Card Generation Due", message);
		}

		log.info("action=report-card-generation-complete tenantId={} eligibleCount={} notifiedCount={}", tenantId,
				eligibleCount, notifiedCount);
		return new JobExecutionResult.Success(
				Map.of("generatedCount", eligibleCount, "notifiedCount", notifiedCount), null);
	}
}
