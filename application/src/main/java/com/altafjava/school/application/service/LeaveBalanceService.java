package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.leave.model.LeaveBalance;
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
}
