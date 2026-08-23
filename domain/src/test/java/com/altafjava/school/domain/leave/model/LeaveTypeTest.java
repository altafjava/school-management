package com.altafjava.school.domain.leave.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LeaveTypeTest {

	@Test
	void create_isActiveByDefault() {
		LeaveType leaveType = LeaveType.create("Sick Leave", BigDecimal.valueOf(12));

		assertTrue(leaveType.isActive());
		assertEquals("Sick Leave", leaveType.getName());
	}

	@Test
	void updateDetails_replacesMutableFields() {
		LeaveType leaveType = LeaveType.create("Sick Leave", BigDecimal.valueOf(12));

		leaveType.updateDetails("Medical Leave", BigDecimal.valueOf(15));

		assertEquals("Medical Leave", leaveType.getName());
		assertEquals(0, BigDecimal.valueOf(15).compareTo(leaveType.getDefaultAnnualDays()));
	}

	@Test
	void deactivate_flipsActiveFlag() {
		LeaveType leaveType = LeaveType.create("Sick Leave", BigDecimal.valueOf(12));

		leaveType.deactivate();

		assertFalse(leaveType.isActive());
	}
}
