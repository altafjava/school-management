package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.counseling.model.CounselingReferral;
import com.altafjava.school.domain.counseling.model.CounselingSession;
import com.altafjava.school.domain.counseling.repository.CounselingReferralRepository;
import com.altafjava.school.domain.counseling.repository.CounselingSessionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class CounselingReferralService {

	private final CounselingReferralRepository counselingReferralRepository;
	private final CounselingSessionRepository counselingSessionRepository;
	private final StudentRepository studentRepository;

	public CounselingReferralService(CounselingReferralRepository counselingReferralRepository,
			CounselingSessionRepository counselingSessionRepository, StudentRepository studentRepository) {
		this.counselingReferralRepository = counselingReferralRepository;
		this.counselingSessionRepository = counselingSessionRepository;
		this.studentRepository = studentRepository;
	}

	@Transactional(readOnly = true)
	public Page<CounselingReferral> listAll(Pageable pageable) {
		return counselingReferralRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<CounselingReferral> listForStudent(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		return counselingReferralRepository.findAllByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public CounselingReferral get(String publicId) {
		return findByPublicId(publicId);
	}

	@Transactional
	public CounselingReferral refer(String studentPublicId, String reason) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));

		CounselingReferral referral = CounselingReferral.refer(student.getId(), resolveCurrentUserId(), reason);
		return counselingReferralRepository.save(referral);
	}

	@Transactional
	public CounselingReferral scheduleWithSession(String publicId, String counselingSessionPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		CounselingReferral referral = findByPublicId(publicId);
		CounselingSession session = counselingSessionRepository
				.findByPublicIdAndTenantId(UUID.fromString(counselingSessionPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Counseling session not found: " + counselingSessionPublicId));
		if (!session.getStudentId().equals(referral.getStudentId())) {
			throw new BusinessException("Counseling session must belong to the same student as the referral");
		}

		referral.scheduleWithSession(session.getId());
		return counselingReferralRepository.save(referral);
	}

	@Transactional
	public CounselingReferral complete(String publicId) {
		CounselingReferral referral = findByPublicId(publicId);
		referral.complete();
		return counselingReferralRepository.save(referral);
	}

	@Transactional
	public CounselingReferral decline(String publicId) {
		CounselingReferral referral = findByPublicId(publicId);
		referral.decline();
		return counselingReferralRepository.save(referral);
	}

	private CounselingReferral findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return counselingReferralRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Counseling referral not found: " + publicId));
	}

	private Long resolveCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user.getId();
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot resolve referring user");
	}
}
