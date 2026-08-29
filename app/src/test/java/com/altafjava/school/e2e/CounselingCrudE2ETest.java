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
 * tenant isolation — across both counseling controllers (sessions, referrals). Gated to
 * {@code TENANT_ADMIN} only (see {@code CounselingSessionController} for the rationale), so the
 * "wrong role" case here is TEACHER — stricter than Hostel/Visitor, which allow TEACHER reads.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CounselingCrudE2ETest extends SchoolIntegrationTestBase {

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
				"Counseling E2E School", "counseling-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
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
				.extract().path("data.publicId");
	}

	private String hireTeacher(String accessToken, Long forTenantId, String employeeCode) {
		return given()
				.header("X-Tenant-ID", forTenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"employeeCode":"%s","firstName":"Jane","lastName":"Doe",
						"email":"%s@school.test","joinDate":"2020-01-01"}
						""".formatted(employeeCode, employeeCode.toLowerCase()))
				.when()
				.post("/api/v1/teachers")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	@Test
	void scheduleThenGetSession_asTenantAdmin_returnsExpectedShape() {
		String accessToken = login();
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-COUN-" + UUID.randomUUID().toString()
				.substring(0, 6));
		String teacherPublicId = hireTeacher(accessToken, tenantId, "EMP-COUN-" + UUID.randomUUID().toString()
				.substring(0, 6));

		String sessionPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","counselorTeacherPublicId":"%s","sessionDate":"2026-05-01",
						"notes":"Discussed exam anxiety","followUpRequired":true}
						""".formatted(studentPublicId, teacherPublicId))
				.when()
				.post("/api/v1/counseling-sessions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.followUpRequired", equalTo(true))
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/api/v1/counseling-sessions/" + sessionPublicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.notes", equalTo("Discussed exam anxiety"));
	}

	@Test
	void scheduleSession_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","counselorTeacherPublicId":"%s","sessionDate":"2026-05-01",
						"notes":"Notes","followUpRequired":false}
						""".formatted(UUID.randomUUID(), UUID.randomUUID()))
				.when()
				.post("/api/v1/counseling-sessions")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void scheduleSession_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","counselorTeacherPublicId":"%s","sessionDate":"2026-05-01",
						"notes":"Notes","followUpRequired":false}
						""".formatted(UUID.randomUUID(), UUID.randomUUID()))
				.when()
				.post("/api/v1/counseling-sessions")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void referThenScheduleThenComplete_asTenantAdmin_returnsExpectedShapes() {
		String accessToken = login();
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-REF-" + UUID.randomUUID().toString()
				.substring(0, 6));
		String teacherPublicId = hireTeacher(accessToken, tenantId, "EMP-REF-" + UUID.randomUUID().toString()
				.substring(0, 6));

		String referralPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","reason":"Struggling academically and socially withdrawn"}
						""".formatted(studentPublicId))
				.when()
				.post("/api/v1/counseling-referrals")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.status", equalTo("PENDING"))
				.extract().path("data.publicId");

		String sessionPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","counselorTeacherPublicId":"%s","sessionDate":"2026-05-01",
						"notes":"Initial session","followUpRequired":false}
						""".formatted(studentPublicId, teacherPublicId))
				.when()
				.post("/api/v1/counseling-sessions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"counselingSessionPublicId":"%s"}
						""".formatted(sessionPublicId))
				.when()
				.patch("/api/v1/counseling-referrals/" + referralPublicId + "/schedule")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("SCHEDULED"));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.patch("/api/v1/counseling-referrals/" + referralPublicId + "/complete")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("COMPLETED"));
	}

	@Test
	void referForCounseling_asTeacherRole_returns403() {
		String teacherToken = authHelper.tokenWithRole(tenantId, "TEACHER");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + teacherToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","reason":"Struggling academically"}
						""".formatted(UUID.randomUUID()))
				.when()
				.post("/api/v1/counseling-referrals")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void counselingSessionCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String studentPublicId = enrollStudent(accessToken, tenantId, "STU-ISO-" + UUID.randomUUID().toString()
				.substring(0, 6));
		String teacherPublicId = hireTeacher(accessToken, tenantId, "EMP-ISO-" + UUID.randomUUID().toString()
				.substring(0, 6));
		String sessionPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"studentPublicId":"%s","counselorTeacherPublicId":"%s","sessionDate":"2026-05-01",
						"notes":"Notes","followUpRequired":false}
						""".formatted(studentPublicId, teacherPublicId))
				.when()
				.post("/api/v1/counseling-sessions")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other Counseling School", "counseling-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.when()
				.get("/api/v1/counseling-sessions/" + sessionPublicId)
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
