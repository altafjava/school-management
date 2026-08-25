package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.CustomFieldDefinitionService;
import com.altafjava.school.application.service.CustomFieldValueService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.customfield.model.CustomFieldType;
import com.altafjava.school.domain.student.model.Student;

/**
 * Verifies that a custom field value set under tenant A is never visible from tenant B — even in
 * the adversarial case where tenant B independently defines a custom field with the exact same
 * {@code fieldKey} and happens to look up the exact same surrogate {@code entityId} tenant A's
 * value was stored against. Isolation here is platform's {@code EntityAttributeService}'s job (it
 * scopes every row by {@code tenantId}), but CLAUDE.md requires this layer to carry its own proof
 * too.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class CustomFieldTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private CustomFieldDefinitionService customFieldDefinitionService;

	@Autowired
	private CustomFieldValueService customFieldValueService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"CF School A", "cf-a-" + suffix, 1L, "admin@cf-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"CF School B", "cf-b-" + suffix, 1L, "admin@cf-b.test", "Password123!", "USD"));
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
	void customFieldValueSetUnderTenantA_isNotVisibleFromTenantB_evenWithMatchingDefinitionAndEntityId() {
		activateTenant(tenantA);
		Student studentA = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Alice",
				"Smith", "alice@cf.test", LocalDate.of(2010, 1, 1));
		customFieldDefinitionService.create(CustomFieldEntityType.STUDENT, "bloodGroup", "Blood Group",
				CustomFieldType.TEXT, false);
		customFieldValueService.setValue(CustomFieldEntityType.STUDENT, studentA.getId(), "bloodGroup", "O+");

		activateTenant(tenantB);
		// Tenant B independently defines the same field key — proves this isn't merely "tenant B
		// never defined the field", but a genuine value-level isolation check.
		customFieldDefinitionService.create(CustomFieldEntityType.STUDENT, "bloodGroup", "Blood Group",
				CustomFieldType.TEXT, false);

		Optional<String> valueUnderTenantB = customFieldValueService.getValue(CustomFieldEntityType.STUDENT,
				studentA.getId(), "bloodGroup");

		assertTrue(valueUnderTenantB.isEmpty(), "Tenant B must not see tenant A's stored custom field value");
	}

	@Test
	void getAllValues_underTenantB_doesNotIncludeTenantAsStoredValue() {
		activateTenant(tenantA);
		Student studentA = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Carol",
				"White", "carol@cf.test", LocalDate.of(2012, 3, 3));
		customFieldDefinitionService.create(CustomFieldEntityType.STUDENT, "allergy", "Allergy",
				CustomFieldType.TEXT, false);
		customFieldValueService.setValue(CustomFieldEntityType.STUDENT, studentA.getId(), "allergy", "Peanuts");

		activateTenant(tenantB);
		customFieldDefinitionService.create(CustomFieldEntityType.STUDENT, "allergy", "Allergy",
				CustomFieldType.TEXT, false);

		var valuesUnderTenantB = customFieldValueService.getAllValues(CustomFieldEntityType.STUDENT,
				studentA.getId());

		assertEquals(1, valuesUnderTenantB.size());
		assertTrue(valuesUnderTenantB.get(0).value() == null,
				"Tenant B's view of tenant A's entityId must show the field as unset, not leak 'Peanuts'");
	}
}
