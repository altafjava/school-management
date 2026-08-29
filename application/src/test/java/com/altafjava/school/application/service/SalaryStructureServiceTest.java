package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.payroll.model.PayComponentAmount;
import com.altafjava.school.domain.payroll.model.PayComponentDefinition;
import com.altafjava.school.domain.payroll.model.PayComponentType;
import com.altafjava.school.domain.payroll.model.SalaryStructure;
import com.altafjava.school.domain.payroll.repository.PayComponentDefinitionRepository;
import com.altafjava.school.domain.payroll.repository.SalaryStructureRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class SalaryStructureServiceTest {

	@Mock
	private SalaryStructureRepository salaryStructureRepository;
	@Mock
	private TeacherRepository teacherRepository;
	@Mock
	private PayComponentDefinitionRepository payComponentDefinitionRepository;

	private SalaryStructureService salaryStructureService;

	@BeforeEach
	void setUp() {
		salaryStructureService = new SalaryStructureService(salaryStructureRepository, teacherRepository,
				payComponentDefinitionRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Teacher teacherWithPublicId(UUID publicId, long id) {
		Teacher teacher = Teacher.create("EMP-1", "Jane", "Doe", "jane@school.test", LocalDate.of(2020, 1, 1));
		teacher.setId(id);
		teacher.setPublicId(publicId);
		return teacher;
	}

	private PayComponentDefinition basicPayDefinition() {
		return PayComponentDefinition.create("BASIC", "Basic Pay", PayComponentType.EARNING, 1);
	}

	private void stubBasicPayDefinition() {
		when(payComponentDefinitionRepository.findByCodeAndTenantId("BASIC", 1L))
				.thenReturn(Optional.of(basicPayDefinition()));
	}

	private List<PayComponentAmount> existingComponents(BigDecimal basicPay) {
		return List.of(new PayComponentAmount("BASIC", "Basic Pay", PayComponentType.EARNING, basicPay));
	}

	@Test
	void create_withNoExistingActiveStructure_savesNewActiveStructure() {
		UUID teacherPublicId = UUID.randomUUID();
		Teacher teacher = teacherWithPublicId(teacherPublicId, 10L);
		when(teacherRepository.findByPublicIdAndTenantId(teacherPublicId, 1L)).thenReturn(Optional.of(teacher));
		when(salaryStructureRepository.findByTeacherIdAndActiveTrueAndTenantId(10L, 1L)).thenReturn(Optional.empty());
		when(salaryStructureRepository.save(any(SalaryStructure.class))).thenAnswer(inv -> inv.getArgument(0));
		stubBasicPayDefinition();

		SalaryStructure result = salaryStructureService.create(teacherPublicId.toString(),
				Map.of("BASIC", BigDecimal.valueOf(50000)), LocalDate.of(2026, 1, 1));

		assertTrue(result.isActive());
		assertEquals(10L, result.getTeacherId());
		verify(salaryStructureRepository, times(1)).save(any(SalaryStructure.class));
	}

	@Test
	void create_withExistingActiveStructure_deactivatesPreviousBeforeSavingNew() {
		UUID teacherPublicId = UUID.randomUUID();
		Teacher teacher = teacherWithPublicId(teacherPublicId, 10L);
		SalaryStructure existing = SalaryStructure.create(10L, existingComponents(BigDecimal.valueOf(40000)),
				LocalDate.of(2025, 1, 1));
		when(teacherRepository.findByPublicIdAndTenantId(teacherPublicId, 1L)).thenReturn(Optional.of(teacher));
		when(salaryStructureRepository.findByTeacherIdAndActiveTrueAndTenantId(10L, 1L))
				.thenReturn(Optional.of(existing));
		when(salaryStructureRepository.save(any(SalaryStructure.class))).thenAnswer(inv -> inv.getArgument(0));
		stubBasicPayDefinition();

		salaryStructureService.create(teacherPublicId.toString(), Map.of("BASIC", BigDecimal.valueOf(60000)),
				LocalDate.of(2026, 1, 1));

		assertEquals(false, existing.isActive());
		verify(salaryStructureRepository, times(2)).save(any(SalaryStructure.class));
	}

	@Test
	void create_withUnknownTeacher_throwsResourceNotFoundException() {
		UUID teacherPublicId = UUID.randomUUID();
		when(teacherRepository.findByPublicIdAndTenantId(teacherPublicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> salaryStructureService.create(teacherPublicId.toString(),
						Map.of("BASIC", BigDecimal.valueOf(50000)),
						LocalDate.of(2026, 1, 1)));
	}

	@Test
	void create_withUnknownComponentCode_throwsBusinessException() {
		UUID teacherPublicId = UUID.randomUUID();
		Teacher teacher = teacherWithPublicId(teacherPublicId, 10L);
		when(teacherRepository.findByPublicIdAndTenantId(teacherPublicId, 1L)).thenReturn(Optional.of(teacher));
		when(payComponentDefinitionRepository.findByCodeAndTenantId("GHOST", 1L)).thenReturn(Optional.empty());

		assertThrows(BusinessException.class,
				() -> salaryStructureService.create(teacherPublicId.toString(),
						Map.of("GHOST", BigDecimal.valueOf(50000)),
						LocalDate.of(2026, 1, 1)));
	}

	@Test
	void create_withInactiveComponentCode_throwsBusinessException() {
		UUID teacherPublicId = UUID.randomUUID();
		Teacher teacher = teacherWithPublicId(teacherPublicId, 10L);
		PayComponentDefinition inactive = basicPayDefinition();
		inactive.setActive(false);
		when(teacherRepository.findByPublicIdAndTenantId(teacherPublicId, 1L)).thenReturn(Optional.of(teacher));
		when(payComponentDefinitionRepository.findByCodeAndTenantId("BASIC", 1L)).thenReturn(Optional.of(inactive));

		assertThrows(BusinessException.class,
				() -> salaryStructureService.create(teacherPublicId.toString(),
						Map.of("BASIC", BigDecimal.valueOf(50000)),
						LocalDate.of(2026, 1, 1)));
	}

	@Test
	void supersede_resolvesTeacherFromCurrentStructureAndDeactivatesIt() {
		UUID currentPublicId = UUID.randomUUID();
		SalaryStructure current = SalaryStructure.create(10L, existingComponents(BigDecimal.valueOf(40000)),
				LocalDate.of(2025, 1, 1));
		current.setPublicId(currentPublicId);
		when(salaryStructureRepository.findByPublicIdAndTenantId(currentPublicId, 1L)).thenReturn(Optional.of(current));
		when(salaryStructureRepository.findByTeacherIdAndActiveTrueAndTenantId(10L, 1L))
				.thenReturn(Optional.of(current));
		when(salaryStructureRepository.save(any(SalaryStructure.class))).thenAnswer(inv -> inv.getArgument(0));
		stubBasicPayDefinition();

		SalaryStructure result = salaryStructureService.supersede(currentPublicId.toString(),
				Map.of("BASIC", BigDecimal.valueOf(60000)), LocalDate.of(2026, 1, 1));

		assertEquals(10L, result.getTeacherId());
		assertEquals(false, current.isActive());
	}
}
