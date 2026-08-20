package com.altafjava.school.application.scheduler;

import java.math.BigDecimal;
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
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.service.AttendanceAlertSettingsService;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.attendance.service.AttendancePercentageCalculator;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs weekly at 07:00 tenant-local time on Mondays.
 * Flags active-roster students whose trailing 30-day attendance falls below the tenant's
 * configured threshold (see {@link AttendanceAlertSettingsService}, default 75%).
 */
@Slf4j
@Component
@ScheduledJob(name = "LowAttendanceAlert", group = "school", description = "Notifies parents/students when trailing 30-day attendance falls below the tenant's threshold", cronExpression = "0 0 7 ? * MON", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class LowAttendanceAlertJob implements JobExecutionStrategy {

	private static final int ROLLING_WINDOW_DAYS = 30;

	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final AttendanceRepository attendanceRepository;
	private final StudentRepository studentRepository;
	private final AttendanceAlertSettingsService attendanceAlertSettingsService;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;
	private final AttendancePercentageCalculator attendancePercentageCalculator = new AttendancePercentageCalculator();

	public LowAttendanceAlertJob(StudentClassroomLinkRepository studentClassroomLinkRepository,
			AttendanceRepository attendanceRepository, StudentRepository studentRepository,
			AttendanceAlertSettingsService attendanceAlertSettingsService,
			StudentNotificationRecipientResolver recipientResolver, NotificationService notificationService) {
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.attendanceRepository = attendanceRepository;
		this.studentRepository = studentRepository;
		this.attendanceAlertSettingsService = attendanceAlertSettingsService;
		this.recipientResolver = recipientResolver;
		this.notificationService = notificationService;
	}

	@Override
	public String jobName() {
		return "LowAttendanceAlert";
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
		log.info("action=low-attendance-alert tenantId={} executionId={}", tenantId, ctx.executionId());

		int thresholdPercent = attendanceAlertSettingsService.getLowThresholdPercent();
		LocalDate to = LocalDate.now();
		AttendanceWindow window = new AttendanceWindow(to.minusDays(ROLLING_WINDOW_DAYS), to);
		List<Long> studentIds = studentClassroomLinkRepository.findDistinctStudentIdsByTenantId(tenantId);

		int alertedCount = 0;
		for (Long studentId : studentIds) {
			if (alertIfBelowThreshold(tenantId, studentId, window, thresholdPercent)) {
				alertedCount++;
			}
		}

		log.info("action=low-attendance-alert-complete tenantId={} studentCount={} alertedCount={}", tenantId,
				studentIds.size(), alertedCount);
		return new JobExecutionResult.Success(Map.of("alertedCount", alertedCount), null);
	}

	private boolean alertIfBelowThreshold(Long tenantId, Long studentId, AttendanceWindow window,
			int thresholdPercent) {
		long totalMarkedDays = attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetween(studentId,
				tenantId, window.from(), window.to());
		if (totalMarkedDays == 0) {
			return false;
		}
		long presentDays = attendanceRepository.countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatus(
				studentId, tenantId, window.from(), window.to(), AttendanceStatus.PRESENT);
		AttendancePercentage attendancePercentage = attendancePercentageCalculator.calculate(presentDays,
				totalMarkedDays);
		if (attendancePercentage.percentage().compareTo(BigDecimal.valueOf(thresholdPercent)) >= 0) {
			return false;
		}
		return studentRepository.findByIdAndTenantId(studentId, tenantId)
				.map(student -> notifyRecipient(tenantId, student, attendancePercentage))
				.orElse(false);
	}

	private boolean notifyRecipient(Long tenantId, Student student, AttendancePercentage attendancePercentage) {
		return recipientResolver.resolve(tenantId, student)
				.map(userId -> {
					String studentName = student.getFirstName() + " " + student.getLastName();
					String percentage = attendancePercentage.percentage().toPlainString();
					notificationService.send(SendNotificationCommand.builder()
							.tenantId(tenantId)
							.userId(userId)
							.type(NotificationType.LOW_ATTENDANCE_ALERT)
							.title("Low Attendance Alert")
							.message(studentName + "'s attendance over the last " + ROLLING_WINDOW_DAYS
									+ " days is " + percentage + "%.")
							.templateVariables(Map.of(
									"studentName", studentName,
									"percentage", percentage,
									"windowDays", String.valueOf(ROLLING_WINDOW_DAYS)))
							.priority(NotificationPriority.HIGH)
							.build());
					return true;
				})
				.orElse(false);
	}

	private record AttendanceWindow(LocalDate from, LocalDate to) {
	}
}
