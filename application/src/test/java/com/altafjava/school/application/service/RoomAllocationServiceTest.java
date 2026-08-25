package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.hostel.model.Room;
import com.altafjava.school.domain.hostel.model.RoomAllocation;
import com.altafjava.school.domain.hostel.repository.RoomAllocationRepository;
import com.altafjava.school.domain.hostel.repository.RoomRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class RoomAllocationServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();
	private static final UUID ROOM_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private RoomAllocationRepository roomAllocationRepository;
	@Mock
	private RoomRepository roomRepository;
	@Mock
	private StudentRepository studentRepository;

	private RoomAllocationService roomAllocationService;

	@BeforeEach
	void setUp() {
		roomAllocationService = new RoomAllocationService(roomAllocationRepository, roomRepository, studentRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	private Room roomWithIdAndCapacity(long id, int capacity) {
		Room room = Room.create(1L, "101", capacity);
		room.setId(id);
		return room;
	}

	@Test
	void allocate_withCapacityAvailable_succeeds() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(roomRepository.findByPublicIdAndTenantId(ROOM_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(roomWithIdAndCapacity(20L, 2)));
		when(roomAllocationRepository.existsByStudentIdAndTenantIdAndAllocatedUntilIsNull(10L, 1L))
				.thenReturn(false);
		when(roomAllocationRepository.countByRoomIdAndTenantIdAndAllocatedUntilIsNull(20L, 1L)).thenReturn(1L);
		when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(inv -> inv.getArgument(0));

		RoomAllocation allocation = assertDoesNotThrow(() -> roomAllocationService.allocate(
				STUDENT_PUBLIC_ID.toString(), ROOM_PUBLIC_ID.toString(), LocalDate.of(2026, 4, 1)));

		assertEquals(10L, allocation.getStudentId());
		assertEquals(20L, allocation.getRoomId());
	}

	@Test
	void allocate_roomAtCapacity_throwsBusinessException() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(roomRepository.findByPublicIdAndTenantId(ROOM_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(roomWithIdAndCapacity(20L, 2)));
		when(roomAllocationRepository.existsByStudentIdAndTenantIdAndAllocatedUntilIsNull(10L, 1L))
				.thenReturn(false);
		when(roomAllocationRepository.countByRoomIdAndTenantIdAndAllocatedUntilIsNull(20L, 1L)).thenReturn(2L);

		assertThrows(BusinessException.class, () -> roomAllocationService.allocate(STUDENT_PUBLIC_ID.toString(),
				ROOM_PUBLIC_ID.toString(), LocalDate.of(2026, 4, 1)));
	}

	@Test
	void allocate_studentAlreadyAllocated_throwsBusinessException() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(roomRepository.findByPublicIdAndTenantId(ROOM_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(roomWithIdAndCapacity(20L, 2)));
		when(roomAllocationRepository.existsByStudentIdAndTenantIdAndAllocatedUntilIsNull(10L, 1L))
				.thenReturn(true);

		assertThrows(BusinessException.class, () -> roomAllocationService.allocate(STUDENT_PUBLIC_ID.toString(),
				ROOM_PUBLIC_ID.toString(), LocalDate.of(2026, 4, 1)));
	}

	@Test
	void vacate_setsAllocatedUntil() {
		UUID allocationPublicId = UUID.randomUUID();
		RoomAllocation allocation = RoomAllocation.create(10L, 20L, LocalDate.of(2026, 4, 1));
		when(roomAllocationRepository.findByPublicIdAndTenantId(allocationPublicId, 1L))
				.thenReturn(Optional.of(allocation));
		when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(inv -> inv.getArgument(0));

		RoomAllocation vacated = roomAllocationService.vacate(allocationPublicId.toString(),
				LocalDate.of(2026, 6, 30));

		assertEquals(LocalDate.of(2026, 6, 30), vacated.getAllocatedUntil());
	}
}
