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
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.teacher.model.EmploymentType;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

	@Mock
	private TeacherRepository teacherRepository;
	@Mock
	private DepartmentRepository departmentRepository;

	private TeacherService teacherService;

	@BeforeEach
	void setUp() {
		teacherService = new TeacherService(teacherRepository, departmentRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void hire_withNewEmployeeCode_succeeds() {
		when(teacherRepository.existsByEmployeeCodeAndTenantId("EMP-1", 1L)).thenReturn(false);
		when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

		Teacher teacher = teacherService.hire("EMP-1", "Jane", "Doe", "jane@school.test", LocalDate.of(2020, 1, 1));

		assertEquals("Jane", teacher.getFirstName());
	}

	@Test
	void hire_withDuplicateEmployeeCode_throwsIllegalArgument() {
		when(teacherRepository.existsByEmployeeCodeAndTenantId("EMP-1", 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> teacherService.hire("EMP-1", "Jane", "Doe", "jane@school.test", LocalDate.of(2020, 1, 1)));
	}

	@Test
	void findByPublicId_missing_throwsResourceNotFound() {
		UUID publicId = UUID.randomUUID();
		when(teacherRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> teacherService.findByPublicId(publicId.toString()));
	}

	@Test
	void updateContactDetails_replacesMutableFields() {
		UUID publicId = UUID.randomUUID();
		Teacher teacher = Teacher.create("EMP-2", "Jane", "Doe", "jane@school.test", LocalDate.of(2020, 1, 1));
		when(teacherRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(teacher));
		when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

		Teacher updated = assertDoesNotThrow(() -> teacherService.updateContactDetails(publicId.toString(), "Janet",
				"Smith", "janet@school.test"));

		assertEquals("Janet", updated.getFirstName());
		assertEquals("Smith", updated.getLastName());
		assertEquals("janet@school.test", updated.getEmail());
	}

	@Test
	void updateHrDetails_withDepartment_resolvesAndAssigns() {
		UUID publicId = UUID.randomUUID();
		UUID departmentPublicId = UUID.randomUUID();
		Teacher teacher = Teacher.create("EMP-3", "Jane", "Doe", "jane@school.test", LocalDate.of(2020, 1, 1));
		Department department = Department.create("Science", "SCI", null);
		department.setId(42L);
		when(teacherRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(teacher));
		when(departmentRepository.findByPublicIdAndTenantId(departmentPublicId, 1L))
				.thenReturn(Optional.of(department));
		when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

		Teacher updated = assertDoesNotThrow(() -> teacherService.updateHrDetails(publicId.toString(),
				departmentPublicId.toString(), "M.Sc. Physics", EmploymentType.FULL_TIME));

		assertEquals(42L, updated.getDepartmentId());
		assertEquals("M.Sc. Physics", updated.getQualification());
		assertEquals(EmploymentType.FULL_TIME, updated.getEmploymentType());
	}

	@Test
	void updateHrDetails_withoutDepartment_leavesDepartmentIdNull() {
		UUID publicId = UUID.randomUUID();
		Teacher teacher = Teacher.create("EMP-4", "Jane", "Doe", "jane@school.test", LocalDate.of(2020, 1, 1));
		when(teacherRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(teacher));
		when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

		Teacher updated = assertDoesNotThrow(
				() -> teacherService.updateHrDetails(publicId.toString(), null, "B.Ed.", EmploymentType.PART_TIME));

		assertEquals(null, updated.getDepartmentId());
		assertEquals("B.Ed.", updated.getQualification());
	}
}
