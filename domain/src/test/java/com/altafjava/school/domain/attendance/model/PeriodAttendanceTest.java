package com.altafjava.school.domain.attendance.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PeriodAttendanceTest {

	@Test
	void create_setsAllFields() {
		LocalDate date = LocalDate.of(2026, 2, 1);

		PeriodAttendance attendance = PeriodAttendance.create(1L, 10L, 100L, date, AttendanceStatus.PRESENT,
				"teacher-1");

		assertEquals(1L, attendance.getStudentId());
		assertEquals(10L, attendance.getClassroomId());
		assertEquals(100L, attendance.getTimetableEntryId());
		assertEquals(date, attendance.getAttendanceDate());
		assertEquals(AttendanceStatus.PRESENT, attendance.getStatus());
		assertEquals("teacher-1", attendance.getMarkedBy());
	}
}
