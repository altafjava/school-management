package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.HostelBuildingService;
import com.altafjava.school.application.service.RoomAllocationService;
import com.altafjava.school.application.service.RoomService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.hostel.model.HostelBuilding;
import com.altafjava.school.domain.hostel.model.Room;
import com.altafjava.school.domain.student.model.Student;

/**
 * Verifies that hostel buildings/rooms/allocations created under tenant A are not visible or
 * actionable from tenant B, and that the room-capacity and one-active-allocation-per-student
 * invariants hold through a real database round trip.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class HostelTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private HostelBuildingService hostelBuildingService;

	@Autowired
	private RoomService roomService;

	@Autowired
	private RoomAllocationService roomAllocationService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"Hostel School A", "hostel-a-" + suffix, 1L, "admin@hostel-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"Hostel School B", "hostel-b-" + suffix, 1L, "admin@hostel-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@Test
	void hostelBuildingCreatedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		HostelBuilding building = hostelBuildingService.create("North Block", "12 Campus Road");
		String buildingPublicId = building.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class, () -> hostelBuildingService.findByPublicId(buildingPublicId),
				"Tenant B must not be able to resolve tenant A's hostel building");
	}

	@Test
	void roomAllocationCreatedUnderTenantA_notVisibleFromTenantB() {
		activateTenant(tenantA);
		HostelBuilding building = hostelBuildingService.create("North Block", "12 Campus Road");
		Room room = roomService.create(building.getPublicId().toString(), "101", 2);
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Alice",
				"Smith", "alice@hostel.test", LocalDate.of(2010, 1, 1));
		var allocation = roomAllocationService.allocate(student.getPublicId().toString(),
				room.getPublicId().toString(), LocalDate.of(2026, 4, 1));
		String roomPublicId = room.getPublicId().toString();
		String allocationPublicId = allocation.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> roomAllocationService.listForRoom(roomPublicId, PageRequest.of(0, 20)),
				"Tenant B must not resolve tenant A's room at all");
		assertThrows(ResourceNotFoundException.class,
				() -> roomAllocationService.vacate(allocationPublicId, LocalDate.of(2026, 6, 30)),
				"Tenant B must not be able to vacate tenant A's room allocation");
	}

	@Test
	void allocate_beyondRoomCapacity_throwsBusinessException() {
		activateTenant(tenantA);
		HostelBuilding building = hostelBuildingService.create("South Block", "20 Campus Road");
		Room room = roomService.create(building.getPublicId().toString(), "201", 1);
		Student first = studentService.enroll("STU-CAP1-" + UUID.randomUUID().toString().substring(0, 6), "Bob",
				"Jones", "bob@hostel.test", LocalDate.of(2010, 2, 2));
		Student second = studentService.enroll("STU-CAP2-" + UUID.randomUUID().toString().substring(0, 6), "Carl",
				"Lee", "carl@hostel.test", LocalDate.of(2010, 3, 3));
		roomAllocationService.allocate(first.getPublicId().toString(), room.getPublicId().toString(),
				LocalDate.of(2026, 4, 1));

		assertThrows(BusinessException.class, () -> roomAllocationService.allocate(second.getPublicId().toString(),
				room.getPublicId().toString(), LocalDate.of(2026, 4, 1)),
				"A second allocation beyond the room's capacity must be rejected");
	}

	@Test
	void allocate_studentAlreadyActivelyAllocated_throwsBusinessException() {
		activateTenant(tenantA);
		HostelBuilding building = hostelBuildingService.create("East Block", "30 Campus Road");
		Room roomOne = roomService.create(building.getPublicId().toString(), "301", 2);
		Room roomTwo = roomService.create(building.getPublicId().toString(), "302", 2);
		Student student = studentService.enroll("STU-DUP-" + UUID.randomUUID().toString().substring(0, 6), "Dana",
				"Kim", "dana@hostel.test", LocalDate.of(2010, 4, 4));
		roomAllocationService.allocate(student.getPublicId().toString(), roomOne.getPublicId().toString(),
				LocalDate.of(2026, 4, 1));

		assertThrows(BusinessException.class, () -> roomAllocationService.allocate(student.getPublicId().toString(),
				roomTwo.getPublicId().toString(), LocalDate.of(2026, 4, 1)),
				"A student with an active allocation must not be allocated a second room");
	}
}
