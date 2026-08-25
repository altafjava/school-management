package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.visitor.model.VisitorLog;
import com.altafjava.school.domain.visitor.repository.VisitorLogRepository;

@ExtendWith(MockitoExtension.class)
class VisitorLogServiceTest {

	private static final UUID HOST_TEACHER_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private VisitorLogRepository visitorLogRepository;
	@Mock
	private TeacherRepository teacherRepository;

	private VisitorLogService visitorLogService;

	@BeforeEach
	void setUp() {
		visitorLogService = new VisitorLogService(visitorLogRepository, teacherRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Teacher teacherWithId(long id) {
		Teacher teacher = Teacher.create("EMP-1", "Jane", "Doe", "jane@school.test", null);
		teacher.setId(id);
		return teacher;
	}

	@Test
	void checkIn_withValidHost_succeeds() {
		when(teacherRepository.findByPublicIdAndTenantId(HOST_TEACHER_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(teacherWithId(20L)));
		when(visitorLogRepository.save(any(VisitorLog.class))).thenAnswer(inv -> inv.getArgument(0));

		VisitorLog log = visitorLogService.checkIn("Alex Ray", "555-0100", "Parent-teacher meeting",
				HOST_TEACHER_PUBLIC_ID.toString());

		assertEquals("Alex Ray", log.getVisitorName());
		assertEquals(20L, log.getHostTeacherId());
	}

	@Test
	void checkIn_unknownHost_throwsResourceNotFoundException() {
		when(teacherRepository.findByPublicIdAndTenantId(HOST_TEACHER_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> visitorLogService.checkIn("Alex Ray", "555-0100",
				"Parent-teacher meeting", HOST_TEACHER_PUBLIC_ID.toString()));
	}

	@Test
	void checkOut_setsCheckOutAt() {
		UUID publicId = UUID.randomUUID();
		VisitorLog log = VisitorLog.checkIn("Alex Ray", "555-0100", "Parent-teacher meeting", 20L,
				LocalDateTime.of(2026, 5, 1, 9, 0));
		when(visitorLogRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(log));
		when(visitorLogRepository.save(any(VisitorLog.class))).thenAnswer(inv -> inv.getArgument(0));

		VisitorLog checkedOut = visitorLogService.checkOut(publicId.toString());

		assertEquals(true, checkedOut.getCheckOutAt() != null);
	}
}
