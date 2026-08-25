package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation. Ownership-based RBAC (a parent viewing only their own linked child) is
 * covered separately in {@code StudentDataAccessE2ETest}, which needs real user linkage rather
 * than the synthetic {@code tokenWithRole} used here for plain role checks.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GuardianCrudE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

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
		var tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Guardian E2E School", "grd-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	@Test
	void createGuardian_asTenantAdmin_returns201() {
		String accessToken = login();

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane@school.test\","
						+ "\"phone\":\"555-0100\"}")
				.when()
				.post("/api/v1/guardians")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("publicId", notNullValue())
				.body("firstName", equalTo("Jane"));
	}

	@Test
	void listGuardians_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/guardians")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void createGuardian_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane2@school.test\","
						+ "\"phone\":\"555-0100\"}")
				.when()
				.post("/api/v1/guardians")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void guardianCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane3@school.test\","
						+ "\"phone\":\"555-0100\"}")
				.when()
				.post("/api/v1/guardians")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		var otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "grd-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/guardians/" + publicId)
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void grantThenRevokeConsent_asTenantAdmin_updatesConsentGivenAt() {
		String accessToken = login();
		String guardianPublicId = createGuardian(accessToken, "consent1-jane@school.test");
		String studentPublicId = createStudent(accessToken, "consent1-STU");
		linkGuardianToStudent(accessToken, guardianPublicId, studentPublicId);

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.when()
				.patch("/api/v1/guardians/" + guardianPublicId + "/students/" + studentPublicId + "/consent/grant")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("consentGivenAt", notNullValue());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.when()
				.patch("/api/v1/guardians/" + guardianPublicId + "/students/" + studentPublicId + "/consent/revoke")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("consentGivenAt", org.hamcrest.Matchers.nullValue());
	}

	@Test
	void grantConsent_asTeacherRole_returns403() {
		String accessToken = login();
		String guardianPublicId = createGuardian(accessToken, "consent2-jane@school.test");
		String studentPublicId = createStudent(accessToken, "consent2-STU");
		linkGuardianToStudent(accessToken, guardianPublicId, studentPublicId);
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.when()
				.patch("/api/v1/guardians/" + guardianPublicId + "/students/" + studentPublicId + "/consent/grant")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	private String createGuardian(String accessToken, String email) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"" + email + "\","
						+ "\"phone\":\"555-0100\"}")
				.when()
				.post("/api/v1/guardians")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");
	}

	private String createStudent(String accessToken, String studentCode) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentCode\":\"" + studentCode + "\",\"firstName\":\"Alice\",\"lastName\":\"Smith\","
						+ "\"email\":\"" + studentCode.toLowerCase() + "@school.test\",\"dateOfBirth\":\"2012-01-01\"}")
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");
	}

	private void linkGuardianToStudent(String accessToken, String guardianPublicId, String studentPublicId) {
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + studentPublicId + "\",\"relationshipType\":\"MOTHER\","
						+ "\"primaryContact\":true}")
				.when()
				.post("/api/v1/guardians/" + guardianPublicId + "/students")
				.then()
				.statusCode(HttpStatus.CREATED.value());
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
