package com.altafjava.school.application.alert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertRuleEvaluator;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.core.model.Pageable;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.model.UserSearchCriteria;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * {@code ruleType = "FEE_DEFAULT_RISK"}: alerts {@code FINANCE}/{@code TENANT_ADMIN} staff — not
 * the parent — when a student's outstanding fee balance exceeds the rule's threshold amount
 * (default 1000, in the tenant's currency). A magnitude-based default-risk signal for school
 * staff, distinct from {@link FeePaymentReminderRuleEvaluator}'s routine parent-facing nudge.
 *
 * <p>
 * Threshold is an amount, not a day count: the current fee domain model
 * ({@code FeeStructure}/{@code FeeBalance}) has no due-date concept to measure "days overdue"
 * against, so a days-based risk signal isn't implementable without a separate fee-schema change —
 * out of scope for this phase (see {@code 20-phase4-implementation-plan.md} §4.4).
 */
@Component
public class FeeDefaultRiskRuleEvaluator implements AlertRuleEvaluator {

	public static final String RULE_TYPE = "FEE_DEFAULT_RISK";
	private static final BigDecimal DEFAULT_THRESHOLD_AMOUNT = BigDecimal.valueOf(1000);

	private final StudentRepository studentRepository;
	private final FeePaymentService feePaymentService;
	private final UserRepository userRepository;

	public FeeDefaultRiskRuleEvaluator(StudentRepository studentRepository, FeePaymentService feePaymentService,
			UserRepository userRepository) {
		this.studentRepository = studentRepository;
		this.feePaymentService = feePaymentService;
		this.userRepository = userRepository;
	}

	@Override
	public String supportedRuleType() {
		return RULE_TYPE;
	}

	@Override
	public List<AlertTrigger> evaluate(AlertRule rule) {
		Long tenantId = rule.getTenantId();
		BigDecimal threshold = rule.getThresholdValue() != null ? rule.getThresholdValue() : DEFAULT_THRESHOLD_AMOUNT;

		List<Student> atRiskStudents = new ArrayList<>();
		for (Student student : studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE,
				tenantId)) {
			if (totalOutstanding(tenantId, student).compareTo(threshold) > 0) {
				atRiskStudents.add(student);
			}
		}
		if (atRiskStudents.isEmpty()) {
			return List.of();
		}

		List<Long> staffUserIds = financeAndTenantAdminUserIds();
		List<AlertTrigger> triggers = new ArrayList<>();
		for (Student student : atRiskStudents) {
			BigDecimal outstanding = totalOutstanding(tenantId, student);
			for (Long staffUserId : staffUserIds) {
				triggers.add(buildTrigger(student, outstanding, staffUserId));
			}
		}
		return triggers;
	}

	private BigDecimal totalOutstanding(Long tenantId, Student student) {
		return feePaymentService.calculateBalanceForStudent(tenantId, student).stream()
				.map(FeeBalance::outstandingBalance)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private List<Long> financeAndTenantAdminUserIds() {
		Map<Long, User> byId = new LinkedHashMap<>();
		usersWithRole(SchoolRoles.FINANCE).forEach(user -> byId.put(user.getId(), user));
		usersWithRole(Roles.TENANT_ADMIN).forEach(user -> byId.put(user.getId(), user));
		return List.copyOf(byId.keySet());
	}

	private List<User> usersWithRole(String role) {
		return userRepository.findAll(new UserSearchCriteria(null, role, null, null), Pageable.of(0, 100)).content();
	}

	private AlertTrigger buildTrigger(Student student, BigDecimal outstanding, Long staffUserId) {
		String studentName = student.getFirstName() + " " + student.getLastName();
		return new AlertTrigger(staffUserId, "Fee Default Risk: " + studentName,
				studentName + "'s outstanding fee balance of " + outstanding + " exceeds the default-risk threshold.",
				Map.of(
						"studentName", studentName,
						"amount", outstanding.toPlainString()),
				NotificationPriority.HIGH);
	}
}
