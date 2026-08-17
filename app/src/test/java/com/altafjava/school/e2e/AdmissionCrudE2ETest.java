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
import com.altafjava.platform.domain.tenant.model.Tenant;
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
class AdmissionCrudE2ETest extends SchoolIntegrationTestBase {

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
		Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Admission E2E School", "adm-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private String submitBody(String applicantFirstName) {
		return "{\"applicantFirstName\":\"" + applicantFirstName + "\",\"applicantLastName\":\"Smith\","
				+ "\"applicantDateOfBirth\":\"2015-01-01\",\"guardianFirstName\":\"Bob\",\"guardianLastName\":"
				+ "\"Smith\",\"guardianEmail\":\"bob-" + UUID.randomUUID().toString().substring(0, 6)
				+ "@family.test\",\"guardianPhone\":\"555-1234\",\"appliedGrade\":\"Grade 3\"}";
	}

	@Test
	void submitAdmission_asTenantAdmin_returns201() {
		String accessToken = login();

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(submitBody("Alice"))
				.when()
				.post("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("publicId", notNullValue())
				.body("status", equalTo("SUBMITTED"));
	}

	@Test
	void listAdmissions_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void submitAdmission_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body(submitBody("Carol"))
				.when()
				.post("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void decideApprove_withoutStudentCode_returns400() {
		String accessToken = login();
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(submitBody("Dave"))
				.when()
				.post("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"outcome\":\"APPROVED\",\"decidedBy\":\"admin\"}")
				.when()
				.patch("/api/v1/admissions/" + publicId + "/decision")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void decideApprove_withStudentCode_enrollsStudentAndReturns200() {
		String accessToken = login();
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(submitBody("Erin"))
				.when()
				.post("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");
		String studentCode = "STU-E2E-" + UUID.randomUUID().toString().substring(0, 6);

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"outcome\":\"APPROVED\",\"decidedBy\":\"admin\",\"studentCode\":\"" + studentCode + "\"}")
				.when()
				.patch("/api/v1/admissions/" + publicId + "/decision")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("status", equalTo("ENROLLED"));
	}

	@Test
	void admissionCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(submitBody("Frank"))
				.when()
				.post("/api/v1/admissions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "adm-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/admissions/" + publicId)
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
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
