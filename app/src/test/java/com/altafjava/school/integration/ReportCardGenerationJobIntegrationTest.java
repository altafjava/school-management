package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.model.Page;
import com.altafjava.platform.core.model.Pageable;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.Notification;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.platform.domain.notification.repository.NotificationRepository;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.platform.domain.scheduler.model.TriggerType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.application.scheduler.ReportCardGenerationJob;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;

/**
 * Proves the ReportCardGeneration scheduler job is genuinely wired to NotificationService end to
 * end: running it against real tenant/student data persists a real Notification row with
 * verifiably correct content, not just a mocked interaction (ROADMAP.md Phase 2 "Validate").
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class ReportCardGenerationJobIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private ReportCardGenerationJob reportCardGenerationJob;

	@Autowired
	private StudentService studentService;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	private Tenant tenant;
	private String adminEmail;

	@BeforeEach
	void createTenant() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		adminEmail = "admin@rpc-" + suffix + ".test";
		tenant = onboardingService.registerTenant(
				new RegisterTenantCommand("Report Card School", "rpc-" + suffix, 1L, adminEmail, "Password123!",
						"USD"));
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
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
	void execute_withActiveStudents_persistsRealNotificationForTenantAdmin() {
		studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Alice", "Smith",
				"alice@rpc.test", LocalDate.of(2010, 1, 1));
		studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Bob", "Jones", "bob@rpc.test",
				LocalDate.of(2011, 2, 2));

		JobExecutionResult result = reportCardGenerationJob.execute(context());

		assertTrue(result instanceof JobExecutionResult.Success, "Job must report success: " + result);

		User admin = userRepository.findByEmail(adminEmail)
				.orElseThrow(() -> new IllegalStateException("Admin user not found: " + adminEmail));
		Page<Notification> notifications = notificationRepository.findByUserId(admin.getId(), Pageable.of(0, 10));

		Notification notification = notifications.content().stream()
				.filter(n -> n.getType() == NotificationType.ANNOUNCEMENT)
				.filter(n -> "Report Card Generation Due".equals(n.getTitle()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Expected a persisted Report Card Generation Due notification"));

		assertEquals(tenant.getId(), notification.getTenantId());
		assertTrue(notification.getMessage().contains("2 active student"),
				"Notification message must reference the real active-student count: " + notification.getMessage());
	}

	@Test
	void execute_withNoActiveStudents_persistsNoNotification() {
		JobExecutionResult result = reportCardGenerationJob.execute(context());

		assertTrue(result instanceof JobExecutionResult.Success, "Job must report success: " + result);

		User admin = userRepository.findByEmail(adminEmail)
				.orElseThrow(() -> new IllegalStateException("Admin user not found: " + adminEmail));
		Page<Notification> notifications = notificationRepository.findByUserId(admin.getId(), Pageable.of(0, 10));

		boolean found = notifications.content().stream()
				.anyMatch(n -> "Report Card Generation Due".equals(n.getTitle()));
		assertFalse(found, "No notification must be sent when there are zero active students");
	}
}
