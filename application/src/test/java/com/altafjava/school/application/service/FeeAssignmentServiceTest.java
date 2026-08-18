package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeeAssignmentScope;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeAssignmentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeeAssignmentServiceTest {

	private static final UUID FEE_STRUCTURE_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private FeeAssignmentRepository feeAssignmentRepository;
	@Mock
	private FeeStructureRepository feeStructureRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private ClassroomRepository classroomRepository;

	private FeeAssignmentService feeAssignmentService;

	@BeforeEach
	void setUp() {
		feeAssignmentService = new FeeAssignmentService(feeAssignmentRepository, feeStructureRepository,
				studentRepository, classroomRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private FeeStructure feeStructureWithId(long id) {
		FeeStructure feeStructure = FeeStructure.create("Tuition", java.math.BigDecimal.valueOf(500),
				FeeFrequency.MONTHLY, "Standard");
		feeStructure.setId(id);
		return feeStructure;
	}

	@Test
	void assign_withNeitherStudentNorClassroom_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> feeAssignmentService.assign(FEE_STRUCTURE_PUBLIC_ID.toString(), null, null));

		verify(feeAssignmentRepository, never()).save(any());
	}

	@Test
	void assign_withBothStudentAndClassroom_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> feeAssignmentService.assign(FEE_STRUCTURE_PUBLIC_ID.toString(),
				UUID.randomUUID().toString(), UUID.randomUUID().toString()));

		verify(feeAssignmentRepository, never()).save(any());
	}

	@Test
	void assign_withNonExistentFeeStructure_throwsResourceNotFound() {
		when(feeStructureRepository.findByPublicIdAndTenantId(FEE_STRUCTURE_PUBLIC_ID, 1L))
				.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> feeAssignmentService
				.assign(FEE_STRUCTURE_PUBLIC_ID.toString(), UUID.randomUUID().toString(), null));
	}

	@Test
	void assign_toStudent_succeeds() {
		UUID studentPublicId = UUID.randomUUID();
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test",
				java.time.LocalDate.of(2010, 1, 1));
		student.setId(5L);
		when(feeStructureRepository.findByPublicIdAndTenantId(FEE_STRUCTURE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(feeStructureWithId(2L)));
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(feeAssignmentRepository.existsByFeeStructureIdAndStudentIdAndTenantId(2L, 5L, 1L)).thenReturn(false);
		when(feeAssignmentRepository.save(any(FeeAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

		FeeAssignment assignment = assertDoesNotThrow(() -> feeAssignmentService
				.assign(FEE_STRUCTURE_PUBLIC_ID.toString(), studentPublicId.toString(), null));

		assertEquals(FeeAssignmentScope.STUDENT, assignment.getScope());
		assertEquals(5L, assignment.getStudentId());
	}

	@Test
	void assign_toStudentAlreadyAssigned_throwsBusinessException() {
		UUID studentPublicId = UUID.randomUUID();
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test",
				java.time.LocalDate.of(2010, 1, 1));
		student.setId(5L);
		when(feeStructureRepository.findByPublicIdAndTenantId(FEE_STRUCTURE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(feeStructureWithId(2L)));
		when(studentRepository.findByPublicIdAndTenantId(studentPublicId, 1L)).thenReturn(Optional.of(student));
		when(feeAssignmentRepository.existsByFeeStructureIdAndStudentIdAndTenantId(2L, 5L, 1L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> feeAssignmentService
				.assign(FEE_STRUCTURE_PUBLIC_ID.toString(), studentPublicId.toString(), null));

		verify(feeAssignmentRepository, never()).save(any());
	}

	@Test
	void assign_toClassroom_succeeds() {
		UUID classroomPublicId = UUID.randomUUID();
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 10L, "2024-25", null);
		classroom.setId(7L);
		when(feeStructureRepository.findByPublicIdAndTenantId(FEE_STRUCTURE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(feeStructureWithId(2L)));
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));
		when(feeAssignmentRepository.existsByFeeStructureIdAndClassroomIdAndTenantId(2L, 7L, 1L)).thenReturn(false);
		when(feeAssignmentRepository.save(any(FeeAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

		FeeAssignment assignment = assertDoesNotThrow(() -> feeAssignmentService
				.assign(FEE_STRUCTURE_PUBLIC_ID.toString(), null, classroomPublicId.toString()));

		assertEquals(FeeAssignmentScope.CLASSROOM, assignment.getScope());
		assertEquals(7L, assignment.getClassroomId());
	}

	@Test
	void revoke_softDeletesAssignment() {
		UUID assignmentPublicId = UUID.randomUUID();
		FeeAssignment assignment = FeeAssignment.forStudent(2L, 5L);
		when(feeAssignmentRepository.findByPublicIdAndTenantId(assignmentPublicId, 1L))
				.thenReturn(Optional.of(assignment));
		when(feeAssignmentRepository.save(any(FeeAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> feeAssignmentService.revoke(assignmentPublicId.toString()));

		assertEquals(true, assignment.isDeleted());
	}

	@Test
	void revoke_missing_throwsResourceNotFound() {
		UUID assignmentPublicId = UUID.randomUUID();
		when(feeAssignmentRepository.findByPublicIdAndTenantId(assignmentPublicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> feeAssignmentService.revoke(assignmentPublicId.toString()));
	}
}
