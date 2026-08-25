package com.altafjava.school.application.scheduler;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.service.PayslipService;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs at 01:00 on the 1st of each month, for the just-completed month. Drafts a {@link
 * com.altafjava.school.domain.payroll.model.Payslip} for every teacher in the tenant —
 * teachers with no active salary structure, or that already have a payslip for the month
 * (re-run after a partial failure), are skipped rather than failing the whole job.
 */
@Slf4j
@Component
@ScheduledJob(name = "PayslipGeneration", group = "school", description = "Generates draft payslips for every active teacher for the completed month", cronExpression = "0 0 1 1 * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class PayslipGenerationJob implements JobExecutionStrategy {

	private final TeacherRepository teacherRepository;
	private final PayslipService payslipService;

	public PayslipGenerationJob(TeacherRepository teacherRepository, PayslipService payslipService) {
		this.teacherRepository = teacherRepository;
		this.payslipService = payslipService;
	}

	@Override
	public String jobName() {
		return "PayslipGeneration";
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
		YearMonth payMonth = YearMonth.now().minusMonths(1);
		log.info("action=payslip-generation tenantId={} payMonth={} executionId={}", tenantId, payMonth,
				ctx.executionId());

		List<Teacher> teachers = teacherRepository.findAllByTenantId(tenantId);
		int generated = 0;
		int skipped = 0;
		for (Teacher teacher : teachers) {
			if (generateForTeacher(teacher, payMonth)) {
				generated++;
			} else {
				skipped++;
			}
		}

		log.info("action=payslip-generation-complete tenantId={} payMonth={} generated={} skipped={}", tenantId,
				payMonth, generated, skipped);
		return new JobExecutionResult.Success(Map.of("generated", generated, "skipped", skipped), null);
	}

	private boolean generateForTeacher(Teacher teacher, YearMonth payMonth) {
		try {
			payslipService.generate(teacher.getId(), payMonth);
			return true;
		} catch (BusinessException e) {
			log.warn("action=payslip-generation-skipped teacherId={} payMonth={} reason={}", teacher.getId(),
					payMonth, e.getMessage());
			return false;
		}
	}
}
