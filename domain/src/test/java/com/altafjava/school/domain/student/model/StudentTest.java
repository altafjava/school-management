package com.altafjava.school.domain.student.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class StudentTest {

	private Student newStudent() {
		return Student.create("STU-001", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
	}

	@Test
	void withdraw_fromActive_transitionsToWithdrawn() {
		Student student = newStudent();

		student.withdraw();

		assertEquals(EnrollmentStatus.WITHDRAWN, student.getEnrollmentStatus());
	}

	@Test
	void withdraw_alreadyGraduated_throwsBusinessException() {
		Student student = newStudent();
		student.graduate();

		assertThrows(BusinessException.class, student::withdraw);
	}

	@Test
	void graduate_fromActive_transitionsToGraduated() {
		Student student = newStudent();

		student.graduate();

		assertEquals(EnrollmentStatus.GRADUATED, student.getEnrollmentStatus());
	}

	@Test
	void graduate_alreadyWithdrawn_throwsBusinessException() {
		Student student = newStudent();
		student.withdraw();

		assertThrows(BusinessException.class, student::graduate);
	}

	@Test
	void transfer_fromActive_transitionsToTransferred() {
		Student student = newStudent();

		student.transfer();

		assertEquals(EnrollmentStatus.TRANSFERRED, student.getEnrollmentStatus());
	}

	@Test
	void transfer_alreadyGraduated_throwsBusinessException() {
		Student student = newStudent();
		student.graduate();

		assertThrows(BusinessException.class, student::transfer);
	}

	@Test
	void graduate_alreadyTransferred_throwsBusinessException() {
		Student student = newStudent();
		student.transfer();

		assertThrows(BusinessException.class, student::graduate);
	}

	@Test
	void updateContactDetails_replacesMutableFields() {
		Student student = newStudent();

		student.updateContactDetails("Alicia", "Jones", "alicia@school.test", LocalDate.of(2010, 2, 2));

		assertEquals("Alicia", student.getFirstName());
		assertEquals("Jones", student.getLastName());
		assertEquals("alicia@school.test", student.getEmail());
		assertEquals(LocalDate.of(2010, 2, 2), student.getDateOfBirth());
	}
}
