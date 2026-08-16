package com.altafjava.school.application.scheduler;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily at 07:00 tenant-local time.
 * Reminds teachers in each tenant to mark attendance for the day.
 * TenantContext is set by the platform scheduler before execute() is called.
 */
@Slf4j
@Component("schoolDailyAttendanceReminderJob")
@ScheduledJob(name = "DailyAttendanceReminder", group = "school", description = "Reminds teachers to mark attendance", cronExpression = "0 0 7 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class DailyAttendanceReminderJob implements JobExecutionStrategy {

	private final ClassroomRepository classroomRepository;
	private final AttendanceRepository attendanceRepository;
	private final TeacherRepository teacherRepository;
	private final NotificationService notificationService;

	public DailyAttendanceReminderJob(ClassroomRepository classroomRepository,
			AttendanceRepository attendanceRepository, TeacherRepository teacherRepository,
			NotificationService notificationService) {
		this.classroomRepository = classroomRepository;
		this.attendanceRepository = attendanceRepository;
		this.teacherRepository = teacherRepository;
		this.notificationService = notificationService;
	}

	@Override
	public String jobName() {
		return "DailyAttendanceReminder";
	}

	@Override
	public String jobGroup() {
		return "school";
	}

	@Override
	public boolean isTenantScoped() {
		return true;
	}

	@Override
	public JobExecutionResult execute(JobExecutionContext ctx) {
		Long tenantId = TenantContext.getCurrentTenantId();
		log.info("action=attendance-reminder tenantId={} executionId={}", tenantId, ctx.executionId());

		LocalDate today = LocalDate.now();
		List<Classroom> classrooms = classroomRepository.findAllByTenantId(tenantId);

		int remindedCount = 0;
		for (Classroom classroom : classrooms) {
			if (classroom.getClassTeacherId() == null) {
				continue;
			}
			boolean alreadyMarked = attendanceRepository.existsByClassroomIdAndAttendanceDateAndTenantId(
					classroom.getId(), today, tenantId);
			if (!alreadyMarked && remindTeacher(tenantId, classroom)) {
				remindedCount++;
			}
		}

		log.info("action=attendance-reminder-complete tenantId={} remindedCount={}", tenantId, remindedCount);
		return new JobExecutionResult.Success(Map.of("remindedCount", remindedCount), null);
	}

	private boolean remindTeacher(Long tenantId, Classroom classroom) {
		Teacher teacher = teacherRepository.findByIdAndTenantId(classroom.getClassTeacherId(), tenantId).orElse(null);
		if (teacher == null || teacher.getUserId() == null) {
			return false;
		}
		notificationService.send(SendNotificationCommand.builder()
				.tenantId(tenantId)
				.userId(teacher.getUserId())
				.type(NotificationType.ANNOUNCEMENT)
				.title("Attendance Reminder")
				.message("Attendance has not yet been marked today for classroom " + classroom.getClassCode() + ".")
				.priority(NotificationPriority.NORMAL)
				.build());
		return true;
	}
}
