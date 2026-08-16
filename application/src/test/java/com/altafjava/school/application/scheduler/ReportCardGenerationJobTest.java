package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.platform.domain.scheduler.model.TriggerType;
import com.altafjava.school.application.scheduler.support.TenantAdminNotifier;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class ReportCardGenerationJobTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private TenantAdminNotifier tenantAdminNotifier;

	private ReportCardGenerationJob job;

	@BeforeEach
	void setUp() {
		job = new ReportCardGenerationJob(studentRepository, tenantAdminNotifier);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "ReportCardGeneration", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	@Test
	void execute_withActiveStudents_notifiesAdminsWithRealCount() {
		when(studentRepository.countByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L)).thenReturn(37L);
		when(tenantAdminNotifier.notifyAll(eq(1L), any(), any())).thenReturn(1);

		JobExecutionResult result = job.execute(context());

		verify(tenantAdminNotifier).notifyAll(eq(1L), any(),
				org.mockito.ArgumentMatchers.contains("37"));
		assertEquals(new JobExecutionResult.Success(Map.of("generatedCount", 37L, "notifiedCount", 1), null), result);
	}

	@Test
	void execute_withNoActiveStudents_doesNotNotify() {
		when(studentRepository.countByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L)).thenReturn(0L);

		JobExecutionResult result = job.execute(context());

		verify(tenantAdminNotifier, never()).notifyAll(any(), any(), any());
		assertEquals(new JobExecutionResult.Success(Map.of("generatedCount", 0L, "notifiedCount", 0), null), result);
	}
}
