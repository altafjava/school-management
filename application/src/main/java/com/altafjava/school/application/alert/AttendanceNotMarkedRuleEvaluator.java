package com.altafjava.school.application.alert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertRuleEvaluator;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

/**
 * {@code ruleType = "DAILY_ATTENDANCE_NOT_MARKED"}: reminds a classroom's class teacher — not a
 * student/guardian — when attendance hasn't been marked yet today (boolean condition, threshold
 * unused). Drives {@code DailyAttendanceReminderJob}.
 */
@Component
public class AttendanceNotMarkedRuleEvaluator implements AlertRuleEvaluator {

	public static final String RULE_TYPE = "DAILY_ATTENDANCE_NOT_MARKED";

	private final ClassroomRepository classroomRepository;
	private final AttendanceRepository attendanceRepository;
	private final TeacherRepository teacherRepository;

	public AttendanceNotMarkedRuleEvaluator(ClassroomRepository classroomRepository,
			AttendanceRepository attendanceRepository, TeacherRepository teacherRepository) {
		this.classroomRepository = classroomRepository;
		this.attendanceRepository = attendanceRepository;
		this.teacherRepository = teacherRepository;
	}

	@Override
	public String supportedRuleType() {
		return RULE_TYPE;
	}

	@Override
	public List<AlertTrigger> evaluate(AlertRule rule) {
		Long tenantId = rule.getTenantId();
		LocalDate today = LocalDate.now();

		List<AlertTrigger> triggers = new ArrayList<>();
		for (Classroom classroom : classroomRepository.findAllByTenantId(tenantId)) {
			if (classroom.getClassTeacherId() == null) {
				continue;
			}
			boolean alreadyMarked = attendanceRepository.existsByClassroomIdAndAttendanceDateAndTenantId(
					classroom.getId(), today, tenantId);
			if (alreadyMarked) {
				continue;
			}
			buildTrigger(tenantId, classroom).ifPresent(triggers::add);
		}
		return triggers;
	}

	private Optional<AlertTrigger> buildTrigger(Long tenantId, Classroom classroom) {
		Teacher teacher = teacherRepository.findByIdAndTenantId(classroom.getClassTeacherId(), tenantId)
				.orElse(null);
		if (teacher == null || teacher.getUserId() == null) {
			return Optional.empty();
		}
		return Optional.of(new AlertTrigger(teacher.getUserId(), "Attendance Reminder",
				"Attendance has not yet been marked today for classroom " + classroom.getClassCode() + ".",
				Map.of(), NotificationPriority.NORMAL));
	}
}
