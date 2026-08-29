package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
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
import com.altafjava.school.application.service.PayslipService;
import com.altafjava.school.application.service.SalaryStructureService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.payroll.model.PayComponentAmount;
import com.altafjava.school.domain.payroll.model.Payslip;
import com.altafjava.school.domain.payroll.model.SalaryStructure;
import com.altafjava.school.domain.teacher.model.Teacher;

/**
 * Verifies that salary structures and payslips created under tenant A are not visible or
 * actionable from tenant B, and that the one-active-structure-per-teacher invariant holds through
 * a real database round trip (not just the mocked unit tests).
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class PayrollTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private SalaryStructureService salaryStructureService;

	@Autowired
	private PayslipService payslipService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"Payroll School A", "payroll-a-" + suffix, 1L, "admin@payroll-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Payroll School B", "payroll-b-" + suffix, 1L, "admin@payroll-b.test", "Password123!", "USD"));
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

	private Teacher hireTeacher(String suffix) {
		return teacherService.hire("EMP-" + suffix, "Jane", "Doe", "jane-" + suffix + "@school.test",
				LocalDate.of(2020, 1, 1));
	}

	private SalaryStructure createStructure(String teacherPublicId) {
		return salaryStructureService.create(teacherPublicId,
				Map.of("BASIC", BigDecimal.valueOf(50000), "HRA", BigDecimal.valueOf(10000), "TRANSPORT",
						BigDecimal.valueOf(2000), "OTHER_ALLOWANCE", BigDecimal.valueOf(500), "OTHER_DEDUCTION",
						BigDecimal.valueOf(1000)),
				LocalDate.of(2026, 1, 1));
	}

	private BigDecimal amountFor(Payslip payslip, String code) {
		return payslip.getComponents().stream()
				.filter(component -> component.code().equals(code))
				.findFirst()
				.map(PayComponentAmount::amount)
				.orElseThrow();
	}

	@Test
	void salaryStructureCreatedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		Teacher teacher = hireTeacher("sal-" + UUID.randomUUID().toString().substring(0, 6));
		SalaryStructure structure = createStructure(teacher.getPublicId().toString());
		String structurePublicId = structure.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> salaryStructureService.findByPublicId(structurePublicId),
				"Tenant B must not be able to resolve tenant A's salary structure");
	}

	@Test
	void payslipGeneratedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		Teacher teacher = hireTeacher("pay-" + UUID.randomUUID().toString().substring(0, 6));
		createStructure(teacher.getPublicId().toString());
		Payslip payslip = payslipService.generate(teacher.getId(), YearMonth.of(2026, 5));
		String payslipPublicId = payslip.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> payslipService.findByPublicId(payslipPublicId),
				"Tenant B must not be able to resolve tenant A's payslip");
	}

	@Test
	void creatingSecondSalaryStructure_deactivatesFirstThroughRealPersistence() {
		activateTenant(tenantA);
		Teacher teacher = hireTeacher("sup-" + UUID.randomUUID().toString().substring(0, 6));
		SalaryStructure first = createStructure(teacher.getPublicId().toString());

		SalaryStructure second = salaryStructureService.create(teacher.getPublicId().toString(),
				Map.of("BASIC", BigDecimal.valueOf(60000)), LocalDate.of(2026, 6, 1));

		SalaryStructure reloadedFirst = salaryStructureService.findByPublicId(first.getPublicId().toString());
		assertFalse(reloadedFirst.isActive(), "Previous salary structure must be deactivated on supersession");
		assertTrue(second.isActive());
	}

	@Test
	void generatingPayslipTwiceForSameTeacherAndMonth_throwsBusinessException() {
		activateTenant(tenantA);
		Teacher teacher = hireTeacher("dup-" + UUID.randomUUID().toString().substring(0, 6));
		createStructure(teacher.getPublicId().toString());
		payslipService.generate(teacher.getId(), YearMonth.of(2026, 5));

		assertThrows(BusinessException.class, () -> payslipService.generate(teacher.getId(), YearMonth.of(2026, 5)),
				"A second payslip for the same teacher/month must be rejected");
	}

	@Test
	void payslipCarriesSnapshotIndependentOfLaterStructureRevision() {
		activateTenant(tenantA);
		Teacher teacher = hireTeacher("snap-" + UUID.randomUUID().toString().substring(0, 6));
		createStructure(teacher.getPublicId().toString());
		Payslip payslip = payslipService.generate(teacher.getId(), YearMonth.of(2026, 5));

		salaryStructureService.create(teacher.getPublicId().toString(), Map.of("BASIC", BigDecimal.valueOf(90000)),
				LocalDate.of(2026, 6, 1));

		Payslip reloaded = payslipService.findByPublicId(payslip.getPublicId().toString());
		assertEquals(0, BigDecimal.valueOf(50000).compareTo(amountFor(reloaded, "BASIC")),
				"Payslip must keep its generation-time snapshot, unaffected by the later revision");
	}
}
