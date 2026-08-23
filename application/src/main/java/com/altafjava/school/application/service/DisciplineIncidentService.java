package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.discipline.model.DisciplineIncident;
import com.altafjava.school.domain.discipline.model.IncidentSeverity;
import com.altafjava.school.domain.discipline.repository.DisciplineIncidentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@Service
public class DisciplineIncidentService {

	private final DisciplineIncidentRepository disciplineIncidentRepository;
	private final StudentRepository studentRepository;
	private final TeacherRepository teacherRepository;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;
	private final StudentDataAccessGuard studentDataAccessGuard;

	public DisciplineIncidentService(DisciplineIncidentRepository disciplineIncidentRepository,
			StudentRepository studentRepository, TeacherRepository teacherRepository,
			StudentNotificationRecipientResolver recipientResolver, NotificationService notificationService,
			StudentDataAccessGuard studentDataAccessGuard) {
		this.disciplineIncidentRepository = disciplineIncidentRepository;
		this.studentRepository = studentRepository;
		this.teacherRepository = teacherRepository;
		this.recipientResolver = recipientResolver;
		this.notificationService = notificationService;
		this.studentDataAccessGuard = studentDataAccessGuard;
	}

	@Transactional(readOnly = true)
	public Page<DisciplineIncident> listAll(Pageable pageable) {
		return disciplineIncidentRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<DisciplineIncident> listForStudent(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return disciplineIncidentRepository.findAllByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional
	public DisciplineIncident record(String studentPublicId, LocalDate incidentDate, IncidentSeverity severity,
			String description) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		Teacher reportingTeacher = resolveCurrentTeacher(tenantId);

		DisciplineIncident incident = DisciplineIncident.report(student.getId(), reportingTeacher.getId(),
				incidentDate, severity, description);
		DisciplineIncident saved = disciplineIncidentRepository.save(incident);
		notifyGuardian(tenantId, student, saved);
		return saved;
	}

	@Transactional
	public DisciplineIncident recordAction(String publicId, String actionTaken) {
		Long tenantId = TenantContext.getCurrentTenantId();
		DisciplineIncident incident = disciplineIncidentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId),
				tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Discipline incident not found: " + publicId));
		incident.recordAction(actionTaken);
		return disciplineIncidentRepository.save(incident);
	}

	private void notifyGuardian(Long tenantId, Student student, DisciplineIncident incident) {
		recipientResolver.resolve(tenantId, student).ifPresent(userId -> {
			notificationService.send(SendNotificationCommand.builder()
					.tenantId(tenantId)
					.userId(userId)
					.type(NotificationType.DISCIPLINE_INCIDENT_RECORDED)
					.title("Discipline Incident Reported")
					.message("A " + incident.getSeverity().name().toLowerCase() + " discipline incident was recorded.")
					.templateVariables(Map.of(
							"studentName", student.getFirstName() + " " + student.getLastName(),
							"severity", incident.getSeverity().name(),
							"incidentDate", incident.getIncidentDate().toString(),
							"description", incident.getDescription()))
					.priority(NotificationPriority.HIGH)
					.build());
			incident.markGuardianNotified();
			disciplineIncidentRepository.save(incident);
		});
	}

	private Teacher resolveCurrentTeacher(Long tenantId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return teacherRepository.findByUserIdAndTenantId(user.getId(), tenantId)
					.orElseThrow(() -> new AccessDeniedException("No teacher record linked to the current user"));
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot resolve reporting teacher");
	}
}
