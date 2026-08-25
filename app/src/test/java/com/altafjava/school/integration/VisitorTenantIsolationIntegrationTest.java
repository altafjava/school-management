package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.application.service.VisitorLogService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.visitor.model.VisitorLog;

/**
 * Verifies that visitor logs created under tenant A are not visible or actionable from tenant B,
 * and that the double-checkout guard holds through a real database round trip.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class VisitorTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private VisitorLogService visitorLogService;

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"Visitor School A", "visitor-a-" + suffix, 1L, "admin@visitor-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Visitor School B", "visitor-b-" + suffix, 1L, "admin@visitor-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@Test
	void visitorLogCreatedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		Teacher host = teacherService.hire("EMP-" + UUID.randomUUID().toString().substring(0, 6), "Jane", "Doe",
				"jane@visitor.test", LocalDate.of(2020, 1, 1));
		VisitorLog log = visitorLogService.checkIn("Alex Ray", "555-0100", "Parent-teacher meeting",
				host.getPublicId().toString());
		String logPublicId = log.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> visitorLogService.findByPublicId(logPublicId),
				"Tenant B must not be able to resolve tenant A's visitor log");
	}

	@Test
	void checkOut_alreadyCheckedOut_isRejectedThroughRealPersistence() {
		activateTenant(tenantA);
		Teacher host = teacherService.hire("EMP-" + UUID.randomUUID().toString().substring(0, 6), "Jane", "Doe",
				"jane2@visitor.test", LocalDate.of(2020, 1, 1));
		VisitorLog log = visitorLogService.checkIn("Sam Fox", "555-0200", "Vendor delivery",
				host.getPublicId().toString());
		String logPublicId = log.getPublicId().toString();

		VisitorLog checkedOut = visitorLogService.checkOut(logPublicId);
		assertTrue(checkedOut.getCheckOutAt() != null);

		assertThrows(BusinessException.class, () -> visitorLogService.checkOut(logPublicId),
				"A visitor already checked out must not be checked out again");
	}
}
