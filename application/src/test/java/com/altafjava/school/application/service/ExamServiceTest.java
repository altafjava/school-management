package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

	@Mock
	private ExamRepository examRepository;
	@Mock
	private ClassroomRepository classroomRepository;

	private ExamService examService;

	@BeforeEach
	void setUp() {
		examService = new ExamService(examRepository, classroomRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void schedule_withNonExistentClassroom_throwsResourceNotFound() {
		when(classroomRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> examService.schedule("Midterm", "Math", 99L, LocalDateTime.now().plusDays(7),
						BigDecimal.valueOf(100)));

		verify(examRepository, never()).save(any());
	}

	@Test
	void schedule_withExistingClassroom_succeeds() {
		when(classroomRepository.existsByIdAndTenantId(10L, 1L)).thenReturn(true);
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> examService.schedule("Midterm", "Math", 10L, LocalDateTime.now().plusDays(7),
				BigDecimal.valueOf(100)));
	}
}
