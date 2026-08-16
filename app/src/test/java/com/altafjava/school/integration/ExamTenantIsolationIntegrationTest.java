package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.ExamService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.exam.model.Exam;

/**
 * Verifies that exam records created under tenant A are not visible to tenant B.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class ExamTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private ExamService examService;

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "exam-a-" + suffix, 1L, "admin@exam-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "exam-b-" + suffix, 1L, "admin@exam-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void examScheduledUnderTenantA_isNotVisibleToTenantB() {
		activateTenant(tenantA);
		Classroom classroom = classroomService.create(
				"CLS-" + UUID.randomUUID().toString().substring(0, 6), "Grade 5", "A", "2024-25", null);
		examService.schedule("Midterm", "Math", classroom.getId(), LocalDateTime.now().plusDays(7),
				BigDecimal.valueOf(100));

		activateTenant(tenantB);
		Page<Exam> tenantBExams = examService.listExams(PageRequest.of(0, 100));

		boolean found = tenantBExams.getContent().stream()
				.anyMatch(e -> tenantA.getId().equals(e.getTenantId()));
		assertFalse(found, "Tenant B must not see exams scheduled under tenant A");
	}

	@Test
	void examPublicId_notAccessibleAcrossTenants() {
		activateTenant(tenantA);
		Classroom classroom = classroomService.create(
				"CLS-" + UUID.randomUUID().toString().substring(0, 6), "Grade 6", "B", "2024-25", null);
		Exam exam = examService.schedule("Final", "Science", classroom.getId(),
				LocalDateTime.now().plusDays(14), BigDecimal.valueOf(100));
		String publicId = exam.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> examService.findByPublicId(publicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's exam");
	}
}
