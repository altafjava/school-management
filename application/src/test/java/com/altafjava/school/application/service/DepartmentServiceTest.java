package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

	@Mock
	private DepartmentRepository departmentRepository;
	@Mock
	private TeacherRepository teacherRepository;

	private DepartmentService departmentService;

	@BeforeEach
	void setUp() {
		departmentService = new DepartmentService(departmentRepository, teacherRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNewCode_succeeds() {
		when(departmentRepository.existsByCodeAndTenantId("SCI", 1L)).thenReturn(false);
		when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

		Department department = departmentService.create("Science", "SCI", "Science department");

		assertEquals("Science", department.getName());
	}

	@Test
	void create_withDuplicateCode_throwsBusinessException() {
		when(departmentRepository.existsByCodeAndTenantId("SCI", 1L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> departmentService.create("Science", "SCI", null));
	}

	@Test
	void assignHeadTeacher_resolvesTeacherAndAssignsId() {
		UUID departmentPublicId = UUID.randomUUID();
		UUID teacherPublicId = UUID.randomUUID();
		Department department = Department.create("Science", "SCI", null);
		Teacher teacher = Teacher.create("EMP-1", "Jane", "Doe", "jane@school.test", null);
		teacher.setId(7L);
		when(departmentRepository.findByPublicIdAndTenantId(departmentPublicId, 1L))
				.thenReturn(Optional.of(department));
		when(teacherRepository.findByPublicIdAndTenantId(teacherPublicId, 1L)).thenReturn(Optional.of(teacher));
		when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

		Department updated = assertDoesNotThrow(() -> departmentService.assignHeadTeacher(
				departmentPublicId.toString(), teacherPublicId.toString()));

		assertEquals(7L, updated.getHeadTeacherId());
	}
}
