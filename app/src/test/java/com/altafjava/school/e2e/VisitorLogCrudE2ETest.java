package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
 * tenant isolation. Gated to {@code TENANT_ADMIN}-or-{@code TEACHER} (see
 * {@code VisitorLogController} for the front-desk-role rationale), so the "wrong role" case here
 * uses the {@code PARENT} role, the closest authenticated-but-unauthorized role available.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VisitorLogCrudE2ETest extends SchoolIntegrationTestBase {

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
				"Visitor E2E School", "visitor-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private String hireTeacher(String accessToken, String employeeCode) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{
						  "employeeCode": "%s",
						  "firstName": "Jane",
						  "lastName": "Doe",
						  "email": "%s@school.test",
						  "joinDate": "2020-08-01"
						}
						""".formatted(employeeCode, employeeCode.toLowerCase()))
				.when()
				.post("/api/v1/teachers")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	@Test
	void checkIn_asTenantAdmin_returns201() {
		String accessToken = login();
		String hostPublicId = hireTeacher(accessToken, "EMP-VIS-" + UUID.randomUUID().toString().substring(0, 6));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"visitorName":"Alex Ray","visitorPhone":"555-0100","purpose":"Parent-teacher meeting",
						"hostTeacherPublicId":"%s"}
						""".formatted(hostPublicId))
				.when()
				.post("/api/v1/visitor-logs")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("data.publicId", notNullValue())
				.body("data.checkOutAt", nullValue());
	}

	@Test
	void checkIn_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("""
						{"visitorName":"Alex Ray","purpose":"Parent-teacher meeting",
						"hostTeacherPublicId":"%s"}
						""".formatted(UUID.randomUUID()))
				.when()
				.post("/api/v1/visitor-logs")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void checkIn_asParentRole_returns403() {
		String parentToken = authHelper.tokenWithRole(tenantId, "PARENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + parentToken)
				.contentType(ContentType.JSON)
				.body("""
						{"visitorName":"Alex Ray","purpose":"Parent-teacher meeting",
						"hostTeacherPublicId":"%s"}
						""".formatted(UUID.randomUUID()))
				.when()
				.post("/api/v1/visitor-logs")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void checkInThenListThenCheckOut_asTenantAdmin_transitionsCorrectly() {
		String accessToken = login();
		String hostPublicId = hireTeacher(accessToken, "EMP-VIS2-" + UUID.randomUUID().toString().substring(0, 6));

		String logPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"visitorName":"Sam Fox","visitorPhone":"555-0200","purpose":"Vendor delivery",
						"hostTeacherPublicId":"%s"}
						""".formatted(hostPublicId))
				.when()
				.post("/api/v1/visitor-logs")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.queryParam("stillCheckedIn", true)
				.when()
				.get("/api/v1/visitor-logs")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.size()", equalTo(1));

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.patch("/api/v1/visitor-logs/" + logPublicId + "/check-out")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.checkOutAt", notNullValue());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.queryParam("stillCheckedIn", true)
				.when()
				.get("/api/v1/visitor-logs")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.size()", equalTo(0));
	}

	@Test
	void checkOut_alreadyCheckedOut_returns400() {
		String accessToken = login();
		String hostPublicId = hireTeacher(accessToken, "EMP-VIS3-" + UUID.randomUUID().toString().substring(0, 6));
		String logPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"visitorName":"Kim Lee","purpose":"Inspection","hostTeacherPublicId":"%s"}
						""".formatted(hostPublicId))
				.when()
				.post("/api/v1/visitor-logs")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.patch("/api/v1/visitor-logs/" + logPublicId + "/check-out")
				.then()
				.statusCode(HttpStatus.OK.value());

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.patch("/api/v1/visitor-logs/" + logPublicId + "/check-out")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void visitorLogCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		String hostPublicId = hireTeacher(accessToken, "EMP-VIS4-" + UUID.randomUUID().toString().substring(0, 6));
		String logPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("""
						{"visitorName":"Nia Park","purpose":"Interview","hostTeacherPublicId":"%s"}
						""".formatted(hostPublicId))
				.when()
				.post("/api/v1/visitor-logs")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other Visitor School", "visitor-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.when()
				.get("/api/v1/visitor-logs/" + logPublicId)
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
