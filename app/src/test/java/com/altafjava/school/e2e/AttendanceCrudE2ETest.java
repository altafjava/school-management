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
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation. See {@code ExamCrudE2ETest}'s class javadoc for why prerequisite IDs are
 * resolved via repository lookup rather than purely through HTTP.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttendanceCrudE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private ClassroomRepository classroomRepository;

	@Autowired
	private StudentRepository studentRepository;

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
				"Attendance E2E School", "att-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
	}

	private Long createClassroomId(String accessToken, String classCode) {
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"classCode\":\"" + classCode + "\",\"grade\":\"Grade 5\",\"section\":\"A\","
						+ "\"academicYear\":\"2025-26\"}")
				.when()
				.post("/api/v1/classrooms")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");
		return resolveClassroomId(publicId);
	}

	private Long createStudentId(String accessToken, String studentCode) {
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentCode\":\"" + studentCode + "\",\"firstName\":\"Alice\",\"lastName\":\"Smith\","
						+ "\"email\":\"alice-" + studentCode + "@school.test\",\"dateOfBirth\":\"2010-01-01\"}")
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");
		return resolveStudentId(publicId);
	}

	private Long resolveClassroomId(String publicId) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return classroomRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
					.orElseThrow().getId();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private Long resolveStudentId(String publicId) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return studentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
					.orElseThrow().getId();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	@Test
	void markAttendance_asTenantAdmin_returns201() {
		String accessToken = login();
		Long classroomId = createClassroomId(accessToken, "CLS-AT1");
		Long studentId = createStudentId(accessToken, "STU-AT1");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentId\":" + studentId + ",\"classroomId\":" + classroomId
						+ ",\"attendanceDate\":\"2026-02-01\",\"status\":\"PRESENT\",\"markedBy\":\"admin\"}")
				.when()
				.post("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("publicId", notNullValue())
				.body("status", equalTo("PRESENT"));
	}

	@Test
	void markAttendance_withInvalidStatus_returns400NotServerError() {
		String accessToken = login();
		Long classroomId = createClassroomId(accessToken, "CLS-AT2");
		Long studentId = createStudentId(accessToken, "STU-AT2");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentId\":" + studentId + ",\"classroomId\":" + classroomId
						+ ",\"attendanceDate\":\"2026-02-01\",\"status\":\"NOT_A_REAL_STATUS\",\"markedBy\":\"admin\"}")
				.when()
				.post("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void listAttendance_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void markAttendance_asStudentRole_returns403() {
		String accessToken = login();
		Long classroomId = createClassroomId(accessToken, "CLS-AT3");
		Long studentId = createStudentId(accessToken, "STU-AT3");
		String studentToken = authHelper.tokenWithRole(tenantId, "STUDENT");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.contentType(ContentType.JSON)
				.body("{\"studentId\":" + studentId + ",\"classroomId\":" + classroomId
						+ ",\"attendanceDate\":\"2026-02-02\",\"status\":\"PRESENT\",\"markedBy\":\"self\"}")
				.when()
				.post("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void attendanceCreatedUnderOneTenant_returns404ForAnotherTenant() {
		String accessToken = login();
		Long classroomId = createClassroomId(accessToken, "CLS-AT4");
		Long studentId = createStudentId(accessToken, "STU-AT4");
		String publicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body("{\"studentId\":" + studentId + ",\"classroomId\":" + classroomId
						+ ",\"attendanceDate\":\"2026-02-03\",\"status\":\"ABSENT\",\"markedBy\":\"admin\"}")
				.when()
				.post("/api/v1/attendance")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "att-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherToken = login(otherTenant.getId(), "admin@" + otherSuffix + ".test", "Password123!");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherToken)
				.contentType(ContentType.JSON)
				.when()
				.get("/api/v1/attendance/" + publicId)
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
