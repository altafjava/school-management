package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import com.altafjava.school.application.service.StudentBulkImportService;
import com.altafjava.school.application.student.BulkImportResult;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * Proves bulk student-roster CSV import is genuinely wired end to end: running it through the
 * real, AOP-proxied ({@code @QuotaCheck}) Spring bean against a real DB persists real Student
 * rows for valid rows and reports per-row failures for invalid ones without aborting the file.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class StudentBulkImportIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private StudentBulkImportService studentBulkImportService;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenant;

	@BeforeEach
	void createTenant() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Bulk Import School", "bulk-" + suffix, 1L, "admin@bulk-" + suffix + ".test", "Password123!",
				"USD"));
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private InputStream csv(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void importCsv_withMixOfValidAndInvalidRows_persistsValidRowsAndReportsFailures() {
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		String content = """
				studentCode,firstName,lastName,email,dateOfBirth
				STU-A-%s,Alice,Smith,alice-%s@school.test,2010-01-15
				STU-B-%s,,Jones,bob-%s@school.test,2011-02-20
				STU-C-%s,Carl,White,carl-%s@school.test,2012-03-10
				""".formatted(suffix, suffix, suffix, suffix, suffix, suffix);

		BulkImportResult result = studentBulkImportService.importCsv(csv(content));

		assertEquals(3, result.totalRows());
		assertEquals(2, result.successCount());
		assertEquals(1, result.failureCount());
		assertTrue(result.failures().get(0).error().contains("firstName"));

		assertTrue(studentRepository.existsByStudentCodeAndTenantId("STU-A-" + suffix, tenant.getId()));
		assertTrue(studentRepository.existsByStudentCodeAndTenantId("STU-C-" + suffix, tenant.getId()));
		assertTrue(studentRepository.findAllByTenantId(tenant.getId(),
				org.springframework.data.domain.PageRequest.of(0, 100)).getContent().stream()
				.noneMatch(s -> s.getStudentCode().equals("STU-B-" + suffix)));
	}

	@Test
	void importCsv_withDuplicateCodeAgainstExistingStudent_reportsRowFailureAndKeepsOriginal() {
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		String code = "STU-DUP-" + suffix;
		String firstFile = """
				studentCode,firstName,lastName,email,dateOfBirth
				%s,Alice,Smith,alice-%s@school.test,2010-01-15
				""".formatted(code, suffix);
		studentBulkImportService.importCsv(csv(firstFile));

		String secondFile = """
				studentCode,firstName,lastName,email,dateOfBirth
				%s,Someone,Else,someone-%s@school.test,2011-01-01
				""".formatted(code, suffix);
		BulkImportResult result = studentBulkImportService.importCsv(csv(secondFile));

		assertEquals(0, result.successCount());
		assertEquals(1, result.failureCount());
		var student = studentRepository.findAllByTenantId(tenant.getId(),
				org.springframework.data.domain.PageRequest.of(0, 100)).getContent().stream()
				.filter(s -> s.getStudentCode().equals(code))
				.findFirst()
				.orElseThrow();
		assertEquals("Alice", student.getFirstName(), "The original row must not be overwritten by the duplicate");
	}
}
