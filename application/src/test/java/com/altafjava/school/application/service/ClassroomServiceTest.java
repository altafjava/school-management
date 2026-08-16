package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private TeacherRepository teacherRepository;

	private ClassroomService classroomService;

	@BeforeEach
	void setUp() {
		classroomService = new ClassroomService(classroomRepository, teacherRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNonExistentClassTeacherId_throwsResourceNotFound() {
		when(teacherRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> classroomService.create("CLS-001", "Grade 5", "A", "2024-25", 99L));

		verify(classroomRepository, never()).save(any());
	}

	@Test
	void create_withExistingClassTeacherId_succeeds() {
		when(teacherRepository.existsByIdAndTenantId(5L, 1L)).thenReturn(true);
		when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> classroomService.create("CLS-001", "Grade 5", "A", "2024-25", 5L));
	}

	@Test
	void create_withNullClassTeacherId_skipsValidation_succeeds() {
		when(classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> classroomService.create("CLS-001", "Grade 5", "A", "2024-25", null));

		verify(teacherRepository, never()).existsByIdAndTenantId(any(), any());
	}
}
