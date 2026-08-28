package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.leave.model.LeaveBalance;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveBalanceRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@Service
public class LeaveBalanceService {

	private final LeaveBalanceRepository leaveBalanceRepository;
	private final TeacherRepository teacherRepository;
	private final AcademicYearRepository academicYearRepository;

	public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository, TeacherRepository teacherRepository,
			AcademicYearRepository academicYearRepository) {
		this.leaveBalanceRepository = leaveBalanceRepository;
		this.teacherRepository = teacherRepository;
		this.academicYearRepository = academicYearRepository;
	}

	@Transactional(readOnly = true)
	public List<LeaveBalance> listForTeacher(String teacherPublicId, String academicYearPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		var teacher = teacherRepository.findByPublicIdAndTenantId(UUID.fromString(teacherPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherPublicId));
		var academicYear = academicYearRepository
				.findByPublicIdAndTenantId(UUID.fromString(academicYearPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + academicYearPublicId));
		return leaveBalanceRepository.findAllByTeacherIdAndAcademicYearIdAndTenantId(teacher.getId(),
				academicYear.getId(), tenantId);
	}

	@Transactional(readOnly = true)
	public List<LeaveBalance> listForCurrentTeacher(String academicYearPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Teacher teacher = resolveCurrentTeacher(tenantId);
		var academicYear = academicYearRepository
				.findByPublicIdAndTenantId(UUID.fromString(academicYearPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + academicYearPublicId));
		return leaveBalanceRepository.findAllByTeacherIdAndAcademicYearIdAndTenantId(teacher.getId(),
				academicYear.getId(), tenantId);
	}

	private Teacher resolveCurrentTeacher(Long tenantId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return teacherRepository.findByUserIdAndTenantId(user.getId(), tenantId)
					.orElseThrow(() -> new AccessDeniedException("No teacher record linked to the current user"));
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot resolve current teacher");
	}

	@Transactional
	public LeaveBalance allocateIfAbsent(Long teacherId, Long leaveTypeId, Long academicYearId,
			BigDecimal allocatedDays) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return leaveBalanceRepository
				.findByTeacherIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(teacherId, leaveTypeId, academicYearId,
						tenantId)
				.orElseGet(() -> leaveBalanceRepository
						.save(LeaveBalance.allocate(teacherId, leaveTypeId, academicYearId, allocatedDays)));
	}

	/**
	 * Same idempotency as {@link #allocateIfAbsent}, additionally bringing forward a capped,
	 * expiry-tracked portion of the teacher's remaining balance for this leave type from the
	 * academic year immediately before {@code academicYear} — only when {@code leaveType} has
	 * carry-forward enabled and a prior balance actually exists. A no-op for an already-allocated
	 * balance, matching {@link #allocateIfAbsent}'s idempotency (carry-forward is only ever applied
	 * once, at initial allocation).
	 */
	@Transactional
	public LeaveBalance allocateWithCarryForward(Teacher teacher, LeaveType leaveType, AcademicYear academicYear) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Optional<LeaveBalance> existing = leaveBalanceRepository
				.findByTeacherIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(
						teacher.getId(), leaveType.getId(), academicYear.getId(), tenantId);
		if (existing.isPresent()) {
			return existing.get();
		}
		LeaveBalance balance = LeaveBalance.allocate(teacher.getId(), leaveType.getId(), academicYear.getId(),
				leaveType.getDefaultAnnualDays());
		if (leaveType.isCarryForwardEnabled()) {
			applyCarryForwardIfEligible(tenantId, teacher, leaveType, academicYear, balance);
		}
		return leaveBalanceRepository.save(balance);
	}

	/**
	 * Forfeits every tenant-wide {@link LeaveBalance} whose carry-forward days have passed their
	 * expiry date and are still (fully or partially) unused — drives
	 * {@code LeaveCarryForwardExpiryJob}. Returns how many balances actually had days forfeited.
	 */
	@Transactional
	public int forfeitExpiredCarryForward(Long tenantId, LocalDate today) {
		List<LeaveBalance> candidates = leaveBalanceRepository
				.findAllByTenantIdAndCarryForwardExpiresAtLessThanEqual(tenantId, today);
		int forfeitedCount = 0;
		for (LeaveBalance balance : candidates) {
			boolean forfeited = balance.forfeitExpiredCarryForward(today).signum() > 0;
			leaveBalanceRepository.save(balance);
			if (forfeited) {
				forfeitedCount++;
			}
		}
		return forfeitedCount;
	}

	private void applyCarryForwardIfEligible(Long tenantId, Teacher teacher, LeaveType leaveType,
			AcademicYear academicYear, LeaveBalance newBalance) {
		academicYearRepository
				.findFirstByTenantIdAndStartDateBeforeOrderByStartDateDesc(tenantId, academicYear.getStartDate())
				.flatMap(previousYear -> leaveBalanceRepository
						.findByTeacherIdAndLeaveTypeIdAndAcademicYearIdAndTenantId(teacher.getId(), leaveType.getId(),
								previousYear.getId(), tenantId))
				.ifPresent(previousBalance -> {
					BigDecimal carryDays = previousBalance.remainingDays().max(BigDecimal.ZERO);
					if (leaveType.getMaxCarryForwardDays() != null) {
						carryDays = carryDays.min(leaveType.getMaxCarryForwardDays());
					}
					if (carryDays.signum() > 0) {
						LocalDate expiresAt = leaveType.getCarryForwardExpiryMonths() != null
								? academicYear.getStartDate().plusMonths(leaveType.getCarryForwardExpiryMonths())
								: null;
						newBalance.applyCarryForward(carryDays, expiresAt);
					}
				});
	}
}
