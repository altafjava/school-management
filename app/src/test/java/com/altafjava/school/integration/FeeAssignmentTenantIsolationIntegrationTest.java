package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.FeeAssignmentService;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.student.model.Student;

/**
 * Verifies that fee assignments are isolated per tenant, and that
 * {@link FeePaymentService#calculateBalance} only returns balances for fee structures the
 * student is actually assigned to (directly, or via their current classroom) — the fix this
 * table exists for (previously every tenant fee structure applied to every student).
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class FeeAssignmentTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private FeeStructureService feeStructureService;

	@Autowired
	private FeeAssignmentService feeAssignmentService;

	@Autowired
	private FeePaymentService feePaymentService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "fea-a-" + suffix, 1L, "admin@fea-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "fea-b-" + suffix, 1L, "admin@fea-b.test", "Password123!", "USD"));
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

	private String createAcademicYear(String name) {
		var academicYear = academicYearService.create(name, LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31),
				true);
		return academicYear.getPublicId().toString();
	}

	@Test
	void assign_referencingAnotherTenantsStudent_isRejected() {
		activateTenant(tenantA);
		Student studentA = studentService.enroll("STU-FEA-1", "Alice", "Smith", "alice@fea-a.test",
				LocalDate.of(2010, 1, 1));

		activateTenant(tenantB);
		FeeStructure feeStructureB = feeStructureService.create("Tuition-B", BigDecimal.valueOf(500),
				FeeFrequency.MONTHLY, "Standard");

		assertThrows(ResourceNotFoundException.class,
				() -> feeAssignmentService.assign(feeStructureB.getPublicId().toString(),
						studentA.getPublicId().toString(), null),
				"Tenant B must not be able to assign its fee structure to tenant A's student");
	}

	@Test
	void feeAssignment_isolatedPerTenant() {
		activateTenant(tenantA);
		FeeStructure feeStructureA = feeStructureService.create("Tuition-A", BigDecimal.valueOf(500),
				FeeFrequency.MONTHLY, "Standard");
		Student studentA = studentService.enroll("STU-FEA-2", "Bob", "Jones", "bob@fea-a.test",
				LocalDate.of(2011, 3, 20));
		feeAssignmentService.assign(feeStructureA.getPublicId().toString(), studentA.getPublicId().toString(), null);

		activateTenant(tenantB);
		FeeStructure feeStructureB = feeStructureService.create("Tuition-B", BigDecimal.valueOf(300),
				FeeFrequency.MONTHLY, "Standard");
		Page<FeeAssignment> assignmentsB = feeAssignmentService.listForFeeStructure(
				feeStructureB.getPublicId().toString(), PageRequest.of(0, 100));

		assertTrue(assignmentsB.getContent().isEmpty(),
				"Tenant B must not see any fee assignments from tenant A");
	}

	@Test
	void calculateBalance_withNoAssignment_returnsNoFeeStructures() {
		activateTenant(tenantA);
		feeStructureService.create("Tuition-Unassigned", BigDecimal.valueOf(500), FeeFrequency.MONTHLY, "Standard");
		Student student = studentService.enroll("STU-FEA-3", "Carol", "Lee", "carol@fea-a.test",
				LocalDate.of(2010, 5, 5));

		var balances = feePaymentService.calculateBalanceForStudent(tenantA.getId(), student);

		assertTrue(balances.isEmpty(),
				"A fee structure with no assignment must not apply to a student — this is the MVP fix");
	}

	@Test
	void calculateBalance_withDirectStudentAssignment_returnsThatStructure() {
		activateTenant(tenantA);
		FeeStructure feeStructure = feeStructureService.create("Tuition-Direct", BigDecimal.valueOf(500),
				FeeFrequency.MONTHLY, "Standard");
		Student student = studentService.enroll("STU-FEA-4", "Dan", "Kim", "dan@fea-a.test",
				LocalDate.of(2010, 7, 7));
		feeAssignmentService.assign(feeStructure.getPublicId().toString(), student.getPublicId().toString(), null);

		var balances = feePaymentService.calculateBalanceForStudent(tenantA.getId(), student);

		assertEquals(1, balances.size());
		FeeBalance balance = balances.get(0);
		assertEquals(feeStructure.getId(), balance.feeStructureId());
		assertEquals(0, BigDecimal.valueOf(500).compareTo(balance.outstandingBalance()));
	}

	@Test
	void calculateBalance_withClassroomAssignment_resolvesViaCurrentClassroom() {
		activateTenant(tenantA);
		String academicYearPublicId = createAcademicYear("2024-25");
		Classroom classroom = classroomService.create("CLS-FEA-1", "Grade 5", "A", academicYearPublicId, null);
		FeeStructure feeStructure = feeStructureService.create("Tuition-Classroom", BigDecimal.valueOf(400),
				FeeFrequency.MONTHLY, "Standard");
		feeAssignmentService.assign(feeStructure.getPublicId().toString(), null,
				classroom.getPublicId().toString());
		Student student = studentService.enroll("STU-FEA-5", "Eve", "Wu", "eve@fea-a.test",
				LocalDate.of(2010, 9, 9));
		classroomService.enrollStudent(classroom.getPublicId().toString(), student.getPublicId().toString(),
				academicYearPublicId);

		var balances = feePaymentService.calculateBalanceForStudent(tenantA.getId(), student);

		assertEquals(1, balances.size());
		assertEquals(feeStructure.getId(), balances.get(0).feeStructureId());
	}
}
