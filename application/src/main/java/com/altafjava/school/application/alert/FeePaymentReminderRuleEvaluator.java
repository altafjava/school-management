package com.altafjava.school.application.alert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertRuleEvaluator;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * {@code ruleType = "FEE_PAYMENT_REMINDER"}: reminds a student's guardian of any outstanding fee
 * balance (threshold unused — any balance greater than zero triggers). Drives {@code
 * FeePaymentReminderJob}. Distinct from {@link FeeDefaultRiskRuleEvaluator}: this notifies the
 * parent as a routine nudge, that notifies staff of a magnitude-based default-risk signal.
 */
@Component
public class FeePaymentReminderRuleEvaluator implements AlertRuleEvaluator {

	public static final String RULE_TYPE = "FEE_PAYMENT_REMINDER";

	private final StudentRepository studentRepository;
	private final FeePaymentService feePaymentService;
	private final StudentNotificationRecipientResolver recipientResolver;

	public FeePaymentReminderRuleEvaluator(StudentRepository studentRepository, FeePaymentService feePaymentService,
			StudentNotificationRecipientResolver recipientResolver) {
		this.studentRepository = studentRepository;
		this.feePaymentService = feePaymentService;
		this.recipientResolver = recipientResolver;
	}

	@Override
	public String supportedRuleType() {
		return RULE_TYPE;
	}

	@Override
	public List<AlertTrigger> evaluate(AlertRule rule) {
		Long tenantId = rule.getTenantId();
		List<AlertTrigger> triggers = new ArrayList<>();
		for (Student student : studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE,
				tenantId)) {
			BigDecimal outstanding = totalOutstanding(tenantId, student);
			if (outstanding.signum() <= 0) {
				continue;
			}
			buildTrigger(tenantId, student, outstanding).ifPresent(triggers::add);
		}
		return triggers;
	}

	private BigDecimal totalOutstanding(Long tenantId, Student student) {
		return feePaymentService.calculateBalanceForStudent(tenantId, student).stream()
				.map(FeeBalance::outstandingBalance)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private Optional<AlertTrigger> buildTrigger(Long tenantId, Student student, BigDecimal outstanding) {
		return recipientResolver.resolve(tenantId, student)
				.map(userId -> {
					String studentName = student.getFirstName() + " " + student.getLastName();
					return new AlertTrigger(userId, "Fee Payment Reminder",
							"Outstanding fee balance for " + studentName + " is " + outstanding
									+ ". Please pay at your earliest convenience.",
							Map.of(
									"studentName", studentName,
									"amount", outstanding.toPlainString()),
							NotificationPriority.NORMAL);
				});
	}
}
