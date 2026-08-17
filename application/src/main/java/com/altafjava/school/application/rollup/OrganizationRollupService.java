package com.altafjava.school.application.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.organization.OrganizationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantContextSnapshot;
import com.altafjava.platform.domain.organization.model.Organization;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.rollup.model.AttendanceRollup;
import com.altafjava.school.domain.rollup.model.CampusRollup;
import com.altafjava.school.domain.rollup.model.FeeRollup;
import com.altafjava.school.domain.rollup.model.OrganizationRollupReport;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * Aggregates attendance, fees, and enrollment across every campus (platform {@code Tenant}) in a
 * school group ({@code Organization}) — see ROADMAP.md Phase 4.
 *
 * <p>
 * Each campus is read by binding {@link TenantContext#callAsTenant} to that campus's snapshot in
 * turn (the platform's own sanctioned "act as tenant X for a bounded scope" API — see its use in
 * {@code AbstractBaseJob} and {@code SchoolTenantProvisioningListener}) and calling the same
 * tenant-scoped repository methods a single-campus request would use. This only produces correct
 * results because the caller of this service is required to hold a system-level (tenant_id=0)
 * token: {@code TenantFilterRequestFilter} only binds Hibernate's session-level {@code
 * tenantFilter} when the resolved request tenant is non-null and non-system. If it were bound to
 * one campus already, nesting {@code callAsTenant} for a different campus would AND two
 * contradictory {@code tenant_id} predicates into the generated SQL and silently return zero rows
 * for every campus but the one the filter is already bound to. {@link OrganizationRollupController}
 * enforces {@code SUPER_ADMIN}, matching platform's own {@code OrganizationController} gating, for
 * exactly this reason.
 */
@Service
public class OrganizationRollupService {

	private static final long MAX_REPORT_RANGE_DAYS = 366;

	private final OrganizationService organizationService;
	private final StudentRepository studentRepository;
	private final AttendanceRepository attendanceRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final FeePaymentRepository feePaymentRepository;

	public OrganizationRollupService(OrganizationService organizationService,
			StudentRepository studentRepository,
			AttendanceRepository attendanceRepository,
			FeeStructureRepository feeStructureRepository,
			FeePaymentRepository feePaymentRepository) {
		this.organizationService = organizationService;
		this.studentRepository = studentRepository;
		this.attendanceRepository = attendanceRepository;
		this.feeStructureRepository = feeStructureRepository;
		this.feePaymentRepository = feePaymentRepository;
	}

	@Transactional(readOnly = true)
	public OrganizationRollupReport generate(String organizationPublicId, LocalDate periodStart, LocalDate periodEnd) {
		validateRange(periodStart, periodEnd);
		UUID publicId = UUID.fromString(organizationPublicId);
		Organization organization = organizationService.findByPublicId(publicId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationPublicId));

		List<Tenant> campuses = organizationService.findTenantsInOrganization(publicId);
		List<CampusRollup> campusRollups = campuses.stream()
				.map(campus -> buildCampusRollup(campus, periodStart, periodEnd))
				.toList();

		return OrganizationRollupReport.of(organization.getPublicId(), organization.getName(), periodStart, periodEnd,
				campusRollups);
	}

	private void validateRange(LocalDate periodStart, LocalDate periodEnd) {
		if (periodEnd.isBefore(periodStart)) {
			throw new BusinessException("'to' date must not be before 'from' date");
		}
		if (ChronoUnit.DAYS.between(periodStart, periodEnd) > MAX_REPORT_RANGE_DAYS) {
			throw new BusinessException("Report period must not exceed " + MAX_REPORT_RANGE_DAYS + " days");
		}
	}

	private CampusRollup buildCampusRollup(Tenant campus, LocalDate periodStart, LocalDate periodEnd) {
		TenantContextSnapshot snapshot = new TenantContextSnapshot(
				campus.getId(), campus.getPublicId(), campus.getSubdomain(), campus.getType(),
				campus.getOrganizationId());
		return TenantContext.<CampusRollup, RuntimeException>callAsTenant(snapshot,
				() -> computeCampusRollup(campus, periodStart, periodEnd));
	}

	private CampusRollup computeCampusRollup(Tenant campus, LocalDate periodStart, LocalDate periodEnd) {
		Long tenantId = campus.getId();
		long activeStudentCount = studentRepository.countByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE,
				tenantId);
		AttendanceRollup attendance = buildAttendanceRollup(tenantId, periodStart, periodEnd);
		FeeRollup fees = buildFeeRollup(tenantId, activeStudentCount);
		return new CampusRollup(campus.getPublicId(), campus.getName(), activeStudentCount, attendance, fees);
	}

	private AttendanceRollup buildAttendanceRollup(Long tenantId, LocalDate periodStart, LocalDate periodEnd) {
		long present = countByStatus(tenantId, periodStart, periodEnd, AttendanceStatus.PRESENT);
		long absent = countByStatus(tenantId, periodStart, periodEnd, AttendanceStatus.ABSENT);
		long late = countByStatus(tenantId, periodStart, periodEnd, AttendanceStatus.LATE);
		long excused = countByStatus(tenantId, periodStart, periodEnd, AttendanceStatus.EXCUSED);
		return new AttendanceRollup(present, absent, late, excused);
	}

	private long countByStatus(Long tenantId, LocalDate periodStart, LocalDate periodEnd, AttendanceStatus status) {
		return attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(tenantId, periodStart, periodEnd,
				status);
	}

	// Fee balance is a snapshot as of now (matching FeeBalanceCalculator/StudentController's own
	// fee-balance endpoint), not scoped to the report's attendance period — every FeeStructure
	// applies to every student (documented assumption, see FeeBalanceCalculator), so total due for
	// a campus is its active student count times the sum of its fee structure amounts.
	private FeeRollup buildFeeRollup(Long tenantId, long activeStudentCount) {
		BigDecimal feeStructureTotal = feeStructureRepository.findAllByTenantId(tenantId).stream()
				.map(FeeStructure::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalDue = feeStructureTotal.multiply(BigDecimal.valueOf(activeStudentCount));
		BigDecimal totalPaid = feePaymentRepository.sumPaidAmountByTenantId(tenantId);
		return FeeRollup.of(totalDue, totalPaid);
	}
}
