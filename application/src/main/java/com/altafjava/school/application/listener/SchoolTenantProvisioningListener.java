package com.altafjava.school.application.listener;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.event.events.TenantCreatedEvent;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantContextSnapshot;
import com.altafjava.platform.domain.notification.model.NotificationChannel;
import com.altafjava.platform.domain.notification.model.NotificationTemplate;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.platform.domain.notification.repository.NotificationTemplateRepository;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Reacts to a new tenant being provisioned by the platform. Seeds school-specific default data
 * (academic year, notification templates). Role seeding is not needed here — TEACHER/STUDENT/PARENT
 * are global system-role templates seeded once via Liquibase, see {@code Role.java}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchoolTenantProvisioningListener {

	private static final Set<NotificationChannel> DEFAULT_CHANNELS = Set.of(NotificationChannel.EMAIL,
			NotificationChannel.IN_APP);

	private static final List<TemplateSeed> NOTIFICATION_TEMPLATE_SEEDS = List.of(
			new TemplateSeed(NotificationType.FEE_DUE, "Fee Payment Reminder",
					"Dear parent/guardian,\n\n{{studentName}} has an outstanding fee balance of {{amount}}. "
							+ "Please make payment at your earliest convenience to avoid any disruption to "
							+ "enrollment.\n\nThank you.",
					List.of("studentName", "amount")),
			new TemplateSeed(NotificationType.EXAM_SCHEDULED, "Upcoming Exam: {{examTitle}}",
					"Dear parent/guardian,\n\n{{studentName}} has a {{subjectName}} exam ({{examTitle}}) "
							+ "scheduled for {{scheduledAt}}. Please ensure they are well prepared.\n\nThank you.",
					List.of("studentName", "examTitle", "subjectName", "scheduledAt")),
			new TemplateSeed(NotificationType.LOW_ATTENDANCE_ALERT, "Low Attendance Alert",
					"Dear parent/guardian,\n\n{{studentName}}'s attendance over the last {{windowDays}} days is "
							+ "{{percentage}}%, which is below the school's minimum threshold. Please contact the "
							+ "school office if you have any concerns.\n\nThank you.",
					List.of("studentName", "percentage", "windowDays")),
			new TemplateSeed(NotificationType.ASSIGNMENT_POSTED, "New Assignment: {{assignmentTitle}}",
					"Dear {{studentName}},\n\nA new assignment \"{{assignmentTitle}}\" has been posted and is "
							+ "due on {{dueDate}}. Please log in to view details and submit your work on "
							+ "time.\n\nThank you.",
					List.of("studentName", "assignmentTitle", "dueDate")),
			new TemplateSeed(NotificationType.SUBMISSION_GRADED, "Submission Graded: {{assignmentTitle}}",
					"Dear student,\n\nYour submission for \"{{assignmentTitle}}\" has been graded. You received "
							+ "{{marksObtained}} marks.\n\nFeedback: {{feedback}}\n\nThank you.",
					List.of("assignmentTitle", "marksObtained", "feedback")));

	private final AcademicYearRepository academicYearRepository;
	private final NotificationTemplateRepository notificationTemplateRepository;
	private final ObjectMapper objectMapper;

	@Async("platformTaskExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@EventListener
	public void onTenantCreated(TenantCreatedEvent event) {
		log.info("action=school-tenant-provisioning tenantId={} tenantType={}", event.tenantId(), event.tenantType());
		TenantContextSnapshot snapshot = new TenantContextSnapshot(
				event.tenantId(), null, null, event.tenantType(), null);
		TenantContext.runAsTenant(snapshot, () -> {
			seedDefaultAcademicYear(event.tenantId());
			seedDefaultNotificationTemplates(event.tenantId());
		});
		log.info("action=school-tenant-provisioning-complete tenantId={}", event.tenantId());
	}

	private void seedDefaultAcademicYear(Long tenantId) {
		LocalDate now = LocalDate.now();
		int year = now.getYear();
		String name = year + "-" + (year + 1);

		if (academicYearRepository.existsByNameAndTenantId(name, tenantId)) {
			log.info("action=seed-academic-year-skipped tenantId={} name={} reason=already-exists", tenantId, name);
			return;
		}

		AcademicYear academicYear = AcademicYear.create(
				name,
				LocalDate.of(year, 4, 1),
				LocalDate.of(year + 1, 3, 31),
				true);
		academicYearRepository.save(academicYear);
		log.info("action=seed-academic-year-created tenantId={} name={}", tenantId, name);
	}

	private void seedDefaultNotificationTemplates(Long tenantId) {
		NOTIFICATION_TEMPLATE_SEEDS.forEach(seed -> seedNotificationTemplate(tenantId, seed));
	}

	private void seedNotificationTemplate(Long tenantId, TemplateSeed seed) {
		if (notificationTemplateRepository.findByTenantIdAndType(tenantId, seed.type()).isPresent()) {
			log.info("action=seed-notification-template-skipped tenantId={} type={} reason=already-exists", tenantId,
					seed.type());
			return;
		}

		NotificationTemplate template = NotificationTemplate.builder()
				.tenantId(tenantId)
				.type(seed.type())
				.subjectTemplate(seed.subject())
				.bodyTemplate(seed.body())
				.channels(DEFAULT_CHANNELS)
				.variables(serializeVariableNames(seed.variableNames()))
				.active(true)
				.build();
		notificationTemplateRepository.save(template);
		log.info("action=seed-notification-template-created tenantId={} type={}", tenantId, seed.type());
	}

	private String serializeVariableNames(List<String> variableNames) {
		try {
			return objectMapper.writeValueAsString(variableNames);
		} catch (JacksonException e) {
			throw new BusinessException("Failed to serialize notification template variables");
		}
	}

	private record TemplateSeed(NotificationType type, String subject, String body, List<String> variableNames) {
	}
}
