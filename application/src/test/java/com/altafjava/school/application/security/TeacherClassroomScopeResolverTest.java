package com.altafjava.school.application.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class TeacherClassroomScopeResolverTest {

	@Mock
	private TeacherRepository teacherRepository;
	@Mock
	private ClassroomRepository classroomRepository;

	private TeacherClassroomScopeResolver resolver;

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private void setUp() {
		resolver = new TeacherClassroomScopeResolver(teacherRepository, classroomRepository);
	}

	private void authenticateAs(Long userId, String authority) {
		AuthenticatedUser principal = new AuthenticatedUser() {
			@Override
			public Long getId() {
				return userId;
			}

			@Override
			public String getUsername() {
				return "user-" + userId;
			}

			@Override
			public Long getTenantId() {
				return 1L;
			}
		};
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authority));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
	}

	@Test
	void resolve_asTenantAdmin_returnsEmptyMeaningNoNarrowing() {
		setUp();
		authenticateAs(1L, "ROLE_TENANT_ADMIN");

		Optional<List<Long>> result = resolver.resolveClassroomIdsIfTeacherScoped(1L);

		assertTrue(result.isEmpty());
	}

	@Test
	void resolve_asTeacherLinkedToClassrooms_returnsTheirClassroomIds() {
		setUp();
		authenticateAs(9L, "ROLE_TEACHER");
		Teacher teacher = Teacher.create("EMP-1", "Jane", "Doe", "jane@school.test", null);
		teacher.setId(20L);
		when(teacherRepository.findByUserIdAndTenantId(9L, 1L)).thenReturn(Optional.of(teacher));
		Classroom classroomA = classroomWithId(100L);
		Classroom classroomB = classroomWithId(101L);
		when(classroomRepository.findAllByClassTeacherIdAndTenantId(20L, 1L))
				.thenReturn(List.of(classroomA, classroomB));

		Optional<List<Long>> result = resolver.resolveClassroomIdsIfTeacherScoped(1L);

		assertTrue(result.isPresent());
		assertEquals(List.of(100L, 101L), result.get());
	}

	@Test
	void resolve_asTeacherWithNoLinkedTeacherRecord_failsClosedWithEmptyList() {
		setUp();
		authenticateAs(9L, "ROLE_TEACHER");
		when(teacherRepository.findByUserIdAndTenantId(9L, 1L)).thenReturn(Optional.empty());

		Optional<List<Long>> result = resolver.resolveClassroomIdsIfTeacherScoped(1L);

		assertTrue(result.isPresent(), "A TEACHER caller must always be scoped, never treated as unrestricted");
		assertTrue(result.get().isEmpty());
	}

	@Test
	void resolve_withNoAuthentication_failsClosedWithEmptyList() {
		setUp();

		Optional<List<Long>> result = resolver.resolveClassroomIdsIfTeacherScoped(1L);

		assertTrue(result.isPresent());
		assertTrue(result.get().isEmpty());
	}

	private Classroom classroomWithId(long id) {
		Classroom classroom = Classroom.create("CLS-" + id, "Grade 5", "A", 1L, "2025-26", null);
		classroom.setId(id);
		return classroom;
	}
}
