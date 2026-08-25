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
 * tenant isolation — across both health controllers (records, incidents). Gated to
 * {@code TENANT_ADMIN} only (see {@code HealthRecordController} for the rationale), so the
 * "wrong role" case here is TEACHER — stricter than Transport/Hostel, which allow TEACHER reads.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthCrudE2ETest extends SchoolIntegrationTestBase {

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
				"Health E2E School", "health-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private String enrollStudent(String accessToken, Long forTenantId, String studentCode) {
		return given()
				.header("X-Tenant-ID", forTenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentCode":"%s","firstName":"Alice","lastName":"Smith",
						"email":"%s@school.test","dateOfBirth":"2010-01-01"}
						""".formatted(studentCode, studentCode.toLowerCase()))
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");
	}

	@Test
	void upsertThenGet_asTenantAdmin_returnsExpectedShape() {
		String accessToken = login();
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-HEALTH-" + UUID.randomUUID().toString()
				.substring(0, 6));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"bloodGroup":"O+","allergies":"Peanuts","conditions":"Asthma","immunizations":"MMR"}
						""")
				.when()
				.put("/api/v1/health-records/students/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("publicId", notNullValue())
				.body("bloodGroup", equalTo("O+"));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/health-records/students/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("allergies", equalTo("Peanuts"));
	}

	@Test
	void upsertHealthRecord_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("""
						{"bloodGroup":"O+","allergies":"Peanuts","conditions":"Asthma","immunizations":"MMR"}
						""")
				.when()
				.put("/api/v1/health-records/students/" + UUID.randomUUID())
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void upsertHealthRecord_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("""
						{"bloodGroup":"O+","allergies":"Peanuts","conditions":"Asthma","immunizations":"MMR"}
						""")
				.when()
				.put("/api/v1/health-records/students/" + UUID.randomUUID())
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void recordThenList_asTenantAdmin_returnsExpectedShape() {
		String accessToken = login();
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-INC-" + UUID.randomUUID().toString()
				.substring(0, 6));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","occurredAt":"2026-05-01T10:00:00","description":"Fell during PE",
						"treatmentGiven":"Ice pack applied"}
						""".formatted(studentPublicId))
				.when()
				.post("/api/v1/medical-incidents")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("publicId", notNullValue())
				.body("description", equalTo("Fell during PE"));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/medical-incidents/students/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("content.size()", equalTo(1));
	}

	@Test
	void recordMedicalIncident_asTeacherRole_returns403() {
		String accessToken = login();
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-INC2-" + UUID.randomUUID().toString()
				.substring(0, 6));
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","occurredAt":"2026-05-01T10:00:00","description":"Fell during PE"}
						""".formatted(studentPublicId))
				.when()
				.post("/api/v1/medical-incidents")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void healthRecordCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-ISO-" + UUID.randomUUID().toString()
				.substring(0, 6));
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"bloodGroup":"O+","allergies":"Peanuts","conditions":"Asthma","immunizations":"MMR"}
						""")
				.when()
				.put("/api/v1/health-records/students/" + studentPublicId)
				.then()
				.statusCode(HttpStatus.OK.value());

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other Health School", "health-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.when()
				.get("/api/v1/health-records/students/" + studentPublicId)
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
