package com.altafjava.school.application.scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs daily at 08:00 tenant-local time.
 * Notifies students and parents of exams scheduled within the next 2 days.
 */
@Slf4j
@Component
@ScheduledJob(name = "ExamScheduleReminder", group = "school", description = "Notifies students and parents of upcoming exams", cronExpression = "0 0 8 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class ExamScheduleReminderJob implements JobExecutionStrategy {

	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm");

	private final ExamRepository examRepository;
	private final SubjectRepository subjectRepository;
	private final AttendanceRepository attendanceRepository;
	private final StudentRepository studentRepository;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;

	public ExamScheduleReminderJob(ExamRepository examRepository, SubjectRepository subjectRepository,
			AttendanceRepository attendanceRepository, StudentRepository studentRepository,
			StudentNotificationRecipientResolver recipientResolver, NotificationService notificationService) {
		this.examRepository = examRepository;
		this.subjectRepository = subjectRepository;
		this.attendanceRepository = attendanceRepository;
		this.studentRepository = studentRepository;
		this.recipientResolver = recipientResolver;
		this.notificationService = notificationService;
	}

	@Override
	public String jobName() {
		return "ExamScheduleReminder";
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
		log.info("action=exam-schedule-reminder tenantId={} executionId={}", tenantId, ctx.executionId());

		LocalDateTime now = LocalDateTime.now();
		List<Exam> upcomingExams = examRepository.findUpcoming(tenantId, now, now.plusDays(2));

		int remindedCount = 0;
		for (Exam exam : upcomingExams) {
			remindedCount += remindStudentsForExam(tenantId, exam);
		}

		log.info("action=exam-schedule-reminder-complete tenantId={} examCount={} remindedCount={}", tenantId,
				upcomingExams.size(), remindedCount);
		return new JobExecutionResult.Success(Map.of("remindedCount", remindedCount), null);
	}

	private int remindStudentsForExam(Long tenantId, Exam exam) {
		Subject subject = subjectRepository.findByIdAndTenantId(exam.getSubjectId(), tenantId).orElse(null);
		if (subject == null) {
			log.warn("action=exam-schedule-reminder-missing-subject tenantId={} examId={} subjectId={}", tenantId,
					exam.getId(), exam.getSubjectId());
			return 0;
		}
		List<Long> studentIds = attendanceRepository.findDistinctStudentIdsByClassroomId(tenantId,
				exam.getClassroomId());

		int remindedCount = 0;
		for (Long studentId : studentIds) {
			Student student = studentRepository.findByIdAndTenantId(studentId, tenantId).orElse(null);
			if (student != null && notifyRecipient(tenantId, exam, subject, student)) {
				remindedCount++;
			}
		}
		return remindedCount;
	}

	private boolean notifyRecipient(Long tenantId, Exam exam, Subject subject, Student student) {
		return recipientResolver.resolve(tenantId, student)
				.map(userId -> {
					String studentName = student.getFirstName() + " " + student.getLastName();
					String scheduledAt = exam.getScheduledAt().format(DATE_TIME_FORMAT);
					notificationService.send(SendNotificationCommand.builder()
							.tenantId(tenantId)
							.userId(userId)
							.type(NotificationType.EXAM_SCHEDULED)
							.title("Upcoming Exam: " + exam.getTitle())
							.message(subject.getName() + " exam for " + studentName + " is scheduled for "
									+ scheduledAt + ".")
							.templateVariables(Map.of(
									"studentName", studentName,
									"examTitle", exam.getTitle(),
									"subjectName", subject.getName(),
									"scheduledAt", scheduledAt))
							.priority(NotificationPriority.NORMAL)
							.build());
					return true;
				})
				.orElse(false);
	}
}
