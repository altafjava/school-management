package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.alert.AlertDispatchService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.platform.domain.scheduler.model.TriggerType;
import com.altafjava.school.application.alert.LibraryOverdueRuleEvaluator;

/** The overdue/grace-days logic itself is tested in {@code LibraryOverdueRuleEvaluatorTest}. */
@ExtendWith(MockitoExtension.class)
class LibraryOverdueReminderJobTest {

	@Mock
	private AlertDispatchService alertDispatchService;

	private LibraryOverdueReminderJob job;

	@BeforeEach
	void setUp() {
		job = new LibraryOverdueReminderJob(alertDispatchService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "LibraryOverdueReminder", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	@Test
	void execute_delegatesToAlertDispatchServiceForLibraryBookOverdueRuleType() {
		when(alertDispatchService.evaluateAndDispatch(1L, LibraryOverdueRuleEvaluator.RULE_TYPE)).thenReturn(4);

		JobExecutionResult result = job.execute(context());

		verify(alertDispatchService).evaluateAndDispatch(1L, LibraryOverdueRuleEvaluator.RULE_TYPE);
		assertEquals(new JobExecutionResult.Success(Map.of("remindedCount", 4), null), result);
	}
}
