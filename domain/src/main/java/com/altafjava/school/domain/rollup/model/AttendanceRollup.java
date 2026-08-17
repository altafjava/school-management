package com.altafjava.school.domain.rollup.model;

import java.util.List;

public record AttendanceRollup(long present, long absent, long late, long excused) {

	public static final AttendanceRollup ZERO = new AttendanceRollup(0, 0, 0, 0);

	public long total() {
		return present + absent + late + excused;
	}

	public static AttendanceRollup sum(List<AttendanceRollup> rollups) {
		return rollups.stream().reduce(ZERO, AttendanceRollup::add);
	}

	private AttendanceRollup add(AttendanceRollup other) {
		return new AttendanceRollup(
				present + other.present,
				absent + other.absent,
				late + other.late,
				excused + other.excused);
	}
}
