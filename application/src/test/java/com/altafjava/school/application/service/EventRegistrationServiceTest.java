package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.domain.event.model.Event;
import com.altafjava.school.domain.event.model.EventRegistration;
import com.altafjava.school.domain.event.model.EventRegistrationStatus;
import com.altafjava.school.domain.event.repository.EventRegistrationRepository;
import com.altafjava.school.domain.event.repository.EventRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceTest {

	private static final UUID EVENT_PUBLIC_ID = UUID.randomUUID();
	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private EventRegistrationRepository eventRegistrationRepository;
	@Mock
	private EventRepository eventRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private NotificationService notificationService;

	private EventRegistrationService eventRegistrationService;

	@BeforeEach
	void setUp() {
		eventRegistrationService = new EventRegistrationService(eventRegistrationRepository, eventRepository,
				studentRepository, recipientResolver, notificationService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Event eventWithId(long id, Integer capacity) {
		Event event = Event.create("Sports Day", null, LocalDateTime.of(2026, 12, 1, 9, 0), null, true, capacity);
		event.setId(id);
		return event;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void register_withCapacityAvailable_succeedsAndNotifies() {
		Event event = eventWithId(5L, 10);
		Student student = studentWithId(10L);
		when(eventRepository.findByPublicIdAndTenantId(EVENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(event));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(eventRegistrationRepository.existsByEventIdAndStudentIdAndTenantIdAndStatus(5L, 10L, 1L,
				EventRegistrationStatus.REGISTERED)).thenReturn(false);
		when(eventRegistrationRepository.countByEventIdAndTenantIdAndStatus(5L, 1L,
				EventRegistrationStatus.REGISTERED)).thenReturn(3L);
		when(eventRegistrationRepository.save(any(EventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(99L));

		EventRegistration registration = assertDoesNotThrow(
				() -> eventRegistrationService.register(EVENT_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString()));

		assertEquals(EventRegistrationStatus.REGISTERED, registration.getStatus());
		verify(notificationService, times(1)).send(any(SendNotificationCommand.class));
	}

	@Test
	void register_atCapacity_throwsBusinessException() {
		Event event = eventWithId(5L, 10);
		Student student = studentWithId(10L);
		when(eventRepository.findByPublicIdAndTenantId(EVENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(event));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(eventRegistrationRepository.existsByEventIdAndStudentIdAndTenantIdAndStatus(5L, 10L, 1L,
				EventRegistrationStatus.REGISTERED)).thenReturn(false);
		when(eventRegistrationRepository.countByEventIdAndTenantIdAndStatus(5L, 1L,
				EventRegistrationStatus.REGISTERED)).thenReturn(10L);

		assertThrows(BusinessException.class,
				() -> eventRegistrationService.register(EVENT_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString()));
	}

	@Test
	void register_alreadyRegistered_throwsBusinessException() {
		Event event = eventWithId(5L, null);
		Student student = studentWithId(10L);
		when(eventRepository.findByPublicIdAndTenantId(EVENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(event));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(eventRegistrationRepository.existsByEventIdAndStudentIdAndTenantIdAndStatus(5L, 10L, 1L,
				EventRegistrationStatus.REGISTERED)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> eventRegistrationService.register(EVENT_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString()));
	}

	@Test
	void register_noCapacityLimit_ignoresCapacityCheck() {
		Event event = eventWithId(5L, null);
		Student student = studentWithId(10L);
		when(eventRepository.findByPublicIdAndTenantId(EVENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(event));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		when(eventRegistrationRepository.existsByEventIdAndStudentIdAndTenantIdAndStatus(5L, 10L, 1L,
				EventRegistrationStatus.REGISTERED)).thenReturn(false);
		when(eventRegistrationRepository.save(any(EventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(
				() -> eventRegistrationService.register(EVENT_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString()));

		verify(eventRegistrationRepository, times(0)).countByEventIdAndTenantIdAndStatus(any(), any(), any());
	}
}
