package com.altafjava.school.application.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class SchoolResourceAccessPolicyTest {

	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private GuardianRepository guardianRepository;
	@Mock
	private StudentGuardianLinkRepository studentGuardianLinkRepository;
	@Mock
	private TeacherRepository teacherRepository;

	private SchoolResourceAccessPolicy policy;

	private void setUp() {
		policy = new SchoolResourceAccessPolicy(classroomRepository, studentRepository, guardianRepository,
				studentGuardianLinkRepository, teacherRepository);
	}

	@Test
	void isAllowed_classroomRead_teacherAssignedToClassroom_allowed() {
		setUp();
		UUID classroomPublicId = UUID.randomUUID();
		Teacher teacher = Teacher.create("EMP-1", "Jane", "Doe", "jane@school.test", null);
		teacher.setId(20L);
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", "2025-26", 20L);
		when(teacherRepository.findByUserIdAndTenantId(9L, 1L)).thenReturn(Optional.of(teacher));
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));

		boolean allowed = policy.isAllowed("9", 1L, ResourceType.CLASSROOM.name(), classroomPublicId.toString(),
				ResourceAction.READ.name());

		assertTrue(allowed);
	}

	@Test
	void isAllowed_classroomRead_teacherAssignedToDifferentClassroom_denied() {
		setUp();
		UUID classroomPublicId = UUID.randomUUID();
		Teacher teacher = Teacher.create("EMP-1", "Jane", "Doe", "jane@school.test", null);
		teacher.setId(20L);
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", "2025-26", 99L);
		when(teacherRepository.findByUserIdAndTenantId(9L, 1L)).thenReturn(Optional.of(teacher));
		when(classroomRepository.findByPublicIdAndTenantId(classroomPublicId, 1L)).thenReturn(Optional.of(classroom));

		boolean allowed = policy.isAllowed("9", 1L, ResourceType.CLASSROOM.name(), classroomPublicId.toString(),
				ResourceAction.READ.name());

		assertFalse(allowed);
	}

	@Test
	void isAllowed_classroomRead_callerHasNoLinkedTeacherRecord_denied() {
		setUp();
		UUID classroomPublicId = UUID.randomUUID();
		when(teacherRepository.findByUserIdAndTenantId(9L, 1L)).thenReturn(Optional.empty());

		boolean allowed = policy.isAllowed("9", 1L, ResourceType.CLASSROOM.name(), classroomPublicId.toString(),
				ResourceAction.READ.name());

		assertFalse(allowed);
	}
}
