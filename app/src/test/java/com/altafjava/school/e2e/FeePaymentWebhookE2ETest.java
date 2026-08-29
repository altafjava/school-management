package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.altafjava.school.config.TestPaymentGatewayConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Exercises {@code FeePaymentWebhookController} directly. The webhook calls themselves carry no
 * JWT, matching real production traffic (the endpoint is {@code permitAll()} and performs no
 * {@code @PreAuthorize}/role check of its own) — an admin token is used only for the ordinary
 * protected setup calls (creating a student/fee-structure/assignment) that precede each test.
 */
@Import({ TestRedisConfig.class, TestPaymentGatewayConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeePaymentWebhookE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private FeePaymentRepository feePaymentRepository;

	private Long tenantId;
	private UUID tenantPublicId;
	private String adminToken;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String adminEmail = "admin-" + suffix + "@school.test";
		String adminPassword = "Password123!";
		Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Webhook E2E School", "wh-e2e-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();
		tenantPublicId = tenant.getPublicId();
		adminToken = login(adminEmail, adminPassword);
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
				.extract().path("data.publicId");
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
				.extract().path("data.publicId");
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

	private boolean feePaymentExistsForReference(String gatewayChargeReference) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, tenantPublicId, null, TenantType.SHARED);
		try {
			return feePaymentRepository.findByGatewayChargeReferenceAndTenantId(gatewayChargeReference, tenantId)
					.isPresent();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private String webhookPayload(String eventType, String gatewayChargeReference, String status,
			String studentPublicId, String feeStructurePublicId) {
		return "{\"eventType\":\"" + eventType + "\",\"gatewayChargeReference\":\"" + gatewayChargeReference
				+ "\",\"status\":\"" + status + "\",\"metadata\":{\"studentPublicId\":\"" + studentPublicId
				+ "\",\"feeStructurePublicId\":\"" + feeStructurePublicId + "\"}}";
	}

	@Test
	void webhook_withInvalidSignature_rejectedBeforeAnyFeePaymentIsCreated() {
		String studentPublicId = createStudent("STU-WH1");
		String feeStructurePublicId = createFeeStructure("Tuition WH1");
		assignFeeStructure(feeStructurePublicId, studentPublicId);
		String gatewayChargeReference = "pi_wh_" + UUID.randomUUID();
		String payload = webhookPayload("payment_intent.succeeded", gatewayChargeReference, "succeeded",
				studentPublicId, feeStructurePublicId);

		given()
				.header("X-Tenant-ID", tenantId)
				.header("Stripe-Signature", "not-the-valid-signature")
				.contentType(ContentType.JSON)
				.body(payload)
				.when()
				.post("/api/v1/fee-payments/webhooks/" + tenantPublicId + "/STRIPE")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value());

		assertEquals(false, feePaymentExistsForReference(gatewayChargeReference));
	}

	@Test
	void webhook_deliveredTwice_isIdempotent_createsOnlyOneFeePayment() {
		String studentPublicId = createStudent("STU-WH2");
		String feeStructurePublicId = createFeeStructure("Tuition WH2");
		assignFeeStructure(feeStructurePublicId, studentPublicId);
		String gatewayChargeReference = "pi_wh_" + UUID.randomUUID();
		String payload = webhookPayload("payment_intent.succeeded", gatewayChargeReference, "succeeded",
				studentPublicId, feeStructurePublicId);

		for (int i = 0; i < 2; i++) {
			given()
					.header("X-Tenant-ID", tenantId)
					.header("Stripe-Signature", TestPaymentGatewayConfig.VALID_SIGNATURE)
					.contentType(ContentType.JSON)
					.body(payload)
					.when()
					.post("/api/v1/fee-payments/webhooks/" + tenantPublicId + "/STRIPE")
					.then()
					.statusCode(HttpStatus.OK.value());
		}

		TenantContext.ForTesting.setCurrentTenant(tenantId, tenantPublicId, null, TenantType.SHARED);
		try {
			long matchCount = feePaymentRepository.findAllByTenantId(tenantId,
					org.springframework.data.domain.PageRequest.of(0, 100)).stream()
					.filter(payment -> gatewayChargeReference.equals(payment.getGatewayChargeReference()))
					.count();
			assertEquals(1, matchCount);
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private String login(String email, String password) {
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			io.restassured.response.Response response = given()
					.header("X-Tenant-ID", tenantId)
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
