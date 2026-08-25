package com.altafjava.school.application.service;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.visitor.model.VisitorLog;
import com.altafjava.school.domain.visitor.repository.VisitorLogRepository;

@Service
public class VisitorLogService {

	private final VisitorLogRepository visitorLogRepository;
	private final TeacherRepository teacherRepository;

	public VisitorLogService(VisitorLogRepository visitorLogRepository, TeacherRepository teacherRepository) {
		this.visitorLogRepository = visitorLogRepository;
		this.teacherRepository = teacherRepository;
	}

	@Transactional(readOnly = true)
	public Page<VisitorLog> list(Boolean stillCheckedIn, LocalDateTime from, LocalDateTime to, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (Boolean.TRUE.equals(stillCheckedIn)) {
			return visitorLogRepository.findAllByTenantIdAndCheckOutAtIsNull(tenantId, pageable);
		}
		if (from != null && to != null) {
			return visitorLogRepository.findAllByTenantIdAndCheckInAtBetween(tenantId, from, to, pageable);
		}
		return visitorLogRepository.findAllByTenantId(tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public VisitorLog findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return visitorLogRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Visitor log not found: " + publicId));
	}

	@Transactional
	public VisitorLog checkIn(String visitorName, String visitorPhone, String purpose, String hostTeacherPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Teacher host = teacherRepository.findByPublicIdAndTenantId(UUID.fromString(hostTeacherPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Host teacher not found: " + hostTeacherPublicId));
		VisitorLog log = VisitorLog.checkIn(visitorName, visitorPhone, purpose, host.getId(), LocalDateTime.now());
		return visitorLogRepository.save(log);
	}

	@Transactional
	public VisitorLog checkOut(String publicId) {
		VisitorLog log = findByPublicId(publicId);
		log.checkOut(LocalDateTime.now());
		return visitorLogRepository.save(log);
	}
}
