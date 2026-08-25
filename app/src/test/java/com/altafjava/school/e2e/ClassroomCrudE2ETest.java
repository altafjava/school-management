package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClassroomCrudE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private AcademicYearService academicYearService;

	private Long tenantId;
	private String adminEmail;
	private String adminPassword;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		adminEmail = "admin-" + suffix + "@school.test";
		adminPassword = "Password123!";
		Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Classroom E2E School", "cls-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	@Test
	void createClassroom_asTenantAdmin_returns201() {
		String accessToken = login();
		String academicYearPublicId = createAcademicYear(tenantId, "2025-26");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{
						  "classCode": "CLS-001",
						  "grade": "Grade 5",
						  "section": "A",
						  "academicYearPublicId": "%s"
						}
						""".formatted(academicYearPublicId))
				.when()
				.post("/api/v1/classrooms")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("publicId", notNullValue())
				.body("classCode", equalTo("CLS-001"));
	}

	@Test
	void listClassrooms_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/classrooms")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void createClassroom_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");
		String academicYearPublicId = createAcademicYear(tenantId, "2025-26");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("""
						{
						  "classCode": "CLS-002",
						  "grade": "Grade 6",
						  "section": "B",
						  "academicYearPublicId": "%s"
						}
						""".formatted(academicYearPublicId))
				.when()
				.post("/api/v1/classrooms")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void classroomCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String academicYearPublicId = createAcademicYear(tenantId, "2025-26");
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{
						  "classCode": "CLS-003",
						  "grade": "Grade 7",
						  "section": "C",
						  "academicYearPublicId": "%s"
						}
						""".formatted(academicYearPublicId))
				.when()
				.post("/api/v1/classrooms")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "cls-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/classrooms/" + publicId)
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void updateCapacity_asTenantAdmin_enforcesOnNextEnrollment() {
		String accessToken = login();
		String academicYearPublicId = createAcademicYear(tenantId, "2025-26");
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{
						  "classCode": "CLS-CAP-E2E",
						  "grade": "Grade 8",
						  "section": "A",
						  "academicYearPublicId": "%s"
						}
						""".formatted(academicYearPublicId))
				.when()
				.post("/api/v1/classrooms")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"capacity\": 1}")
				.when()
				.patch("/api/v1/classrooms/" + publicId + "/capacity")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("capacity", equalTo(1));
	}

	@Test
	void updateCapacity_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");
		String accessToken = login();
		String academicYearPublicId = createAcademicYear(tenantId, "2025-26");
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{
						  "classCode": "CLS-CAP-E2E-2",
						  "grade": "Grade 8",
						  "section": "B",
						  "academicYearPublicId": "%s"
						}
						""".formatted(academicYearPublicId))
				.when()
				.post("/api/v1/classrooms")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("{\"capacity\": 1}")
				.when()
				.patch("/api/v1/classrooms/" + publicId + "/capacity")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	private String createAcademicYear(Long forTenantId, String name) {
		TenantContext.ForTesting.setCurrentTenant(forTenantId, null, null, TenantType.SHARED);
		try {
			return academicYearService.create(name, LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), true)
					.getPublicId().toString();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private String login() {
		return login(tenantId, adminEmail, adminPassword);
	}

	private String login(Long forTenantId, String email, String password) {
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			io.restassured.response.Response response = given()
					.header("X-Tenant-ID", forTenantId)
					.contentType(ContentType.JSON)
					.body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
					.when()
					.post("/api/v1/auth/login");
			if (response.statusCode() == HttpStatus.OK.value()) {
				return response.then().extract().path("data.accessToken");
			}
			if (System.currentTimeMillis() >= deadline) {
				response.then().statusCode(HttpStatus.OK.value());
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		throw new IllegalStateException("login timed out");
	}
}
