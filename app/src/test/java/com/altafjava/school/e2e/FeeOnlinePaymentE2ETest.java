package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
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
import com.altafjava.platform.core.security.PasswordEncoder;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.model.UserStatus;
import com.altafjava.platform.domain.user.repository.RoleRepository;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentGatewayConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Per-controller E2E minimum (CLAUDE.md): happy path, unauthenticated -> 401, wrong role -> 403,
 * tenant isolation, for the parent/student self-service fee payment endpoints.
 */
@Import({ TestRedisConfig.class, TestPaymentGatewayConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeeOnlinePaymentE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Long tenantId;
	private String adminEmail;
	private String adminPassword;
	private String adminToken;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		adminEmail = "admin-" + suffix + "@school.test";
		adminPassword = "Password123!";
		Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"FeeOnlinePayment E2E School", "fop-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
		adminToken = login(tenantId, adminEmail, adminPassword);
	}

	private String createStudent(String studentCode) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"studentCode\":\"" + studentCode + "\",\"firstName\":\"Alice\",\"lastName\":\"Smith\","
						+ "\"email\":\"alice-" + studentCode + "@school.test\",\"dateOfBirth\":\"2010-01-01\"}")
				.when()
				.post("/api/v1/students")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");
	}

	private String createFeeStructure(String name) {
		return given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"name\":\"" + name + "\",\"amount\":500.00,\"frequency\":\"MONTHLY\","
						+ "\"planType\":\"Standard\"}")
				.when()
				.post("/api/v1/fee-structures")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.extract().path("publicId");
	}

	private void assignFeeStructure(String feeStructurePublicId, String studentPublicId) {
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + studentPublicId + "\"}")
				.when()
				.post("/api/v1/fee-structures/" + feeStructurePublicId + "/assignments")
				.then()
				.statusCode(HttpStatus.CREATED.value());
	}

	private Long createStudentUser(String email) {
		return withTenant(() -> {
			var role = roleRepository.findAll().stream()
					.filter(r -> r.getTenantId() == null && "STUDENT".equals(r.getName()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("STUDENT role not seeded"));
			User user = User.builder()
					.email(email)
					.passwordHash(passwordEncoder.encode("Password123!"))
					.status(UserStatus.ACTIVE)
					.emailVerified(true)
					.build();
			user.addRole(role);
			return userRepository.save(user).getId();
		});
	}

	private void linkStudentToUser(String studentPublicId, Long userId) {
		withTenant(() -> {
			var student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
					.orElseThrow();
			student.setUserId(userId);
			studentRepository.save(student);
			return null;
		});
	}

	private <T> T withTenant(java.util.function.Supplier<T> action) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return action.get();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	@Test
	void selfService_studentCreatesConfirmsAndReadsOwnReceipt() {
		String studentPublicId = createStudent("STU-SS1");
		String feeStructurePublicId = createFeeStructure("Tuition SS1");
		assignFeeStructure(feeStructurePublicId, studentPublicId);
		String email = "student-ss1-" + UUID.randomUUID().toString().substring(0, 6) + "@school.test";
		Long studentUserId = createStudentUser(email);
		linkStudentToUser(studentPublicId, studentUserId);
		String studentToken = authHelper.tokenForUser(tenantId, studentUserId, email, "STUDENT");

		String gatewayChargeReference = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + studentPublicId + "\",\"feeStructurePublicId\":\""
						+ feeStructurePublicId + "\"}")
				.when()
				.post("/api/v1/fee-payments/self-service/charges")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("gatewayChargeReference", notNullValue())
				.body("clientSecret", notNullValue())
				.extract().path("gatewayChargeReference");

		String receiptPublicId = given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + studentPublicId + "\",\"feeStructurePublicId\":\""
						+ feeStructurePublicId + "\"}")
				.when()
				.post("/api/v1/fee-payments/self-service/charges/" + gatewayChargeReference + "/confirm")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("receiptNumber", startsWith("GTW-"))
				.body("paidAmount", equalTo(500.0f))
				.extract().path("publicId");

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + studentToken)
				.when()
				.get("/api/v1/fee-payments/self-service/" + receiptPublicId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("publicId", equalTo(receiptPublicId));
	}

	@Test
	void createCharge_withoutJwt_returns401() {
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + UUID.randomUUID() + "\",\"feeStructurePublicId\":\""
						+ UUID.randomUUID() + "\"}")
				.when()
				.post("/api/v1/fee-payments/self-service/charges")
				.then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void createCharge_asTenantAdminRole_returns403() {
		given()
				.header("X-Tenant-ID", tenantId)
				.header("Authorization", "Bearer " + adminToken)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + UUID.randomUUID() + "\",\"feeStructurePublicId\":\""
						+ UUID.randomUUID() + "\"}")
				.when()
				.post("/api/v1/fee-payments/self-service/charges")
				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void createCharge_forAnotherTenantsStudent_returns404() {
		String studentPublicId = createStudent("STU-SS2");
		String feeStructurePublicId = createFeeStructure("Tuition SS2");
		assignFeeStructure(feeStructurePublicId, studentPublicId);

		String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant otherTenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Other School", "fop-other-" + otherSuffix, 1L,
				"admin@" + otherSuffix + ".test", "Password123!", "USD"));
		String otherStudentToken = authHelper.tokenWithRole(otherTenant.getId(), "STUDENT");

		given()
				.header("X-Tenant-ID", otherTenant.getId())
				.header("Authorization", "Bearer " + otherStudentToken)
				.header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(ContentType.JSON)
				.body("{\"studentPublicId\":\"" + studentPublicId + "\",\"feeStructurePublicId\":\""
						+ feeStructurePublicId + "\"}")
				.when()
				.post("/api/v1/fee-payments/self-service/charges")
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value());
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
