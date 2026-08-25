package com.altafjava.school.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.certificate.model.CertificateTemplate;
import com.altafjava.school.domain.certificate.repository.CertificateTemplateRepository;

// Tenant-admin CRUD for certificate templates — same shape as LeaveTypeService.
@Service
public class CertificateTemplateService {

	private final CertificateTemplateRepository certificateTemplateRepository;

	public CertificateTemplateService(CertificateTemplateRepository certificateTemplateRepository) {
		this.certificateTemplateRepository = certificateTemplateRepository;
	}

	@Transactional(readOnly = true)
	public Page<CertificateTemplate> list(Pageable pageable) {
		return certificateTemplateRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public List<CertificateTemplate> listActive() {
		return certificateTemplateRepository.findAllByTenantIdAndActiveTrue(TenantContext.getCurrentTenantId());
	}

	@Transactional(readOnly = true)
	public CertificateTemplate findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return certificateTemplateRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Certificate template not found: " + publicId));
	}

	@Transactional
	public CertificateTemplate create(String name, String bodyTemplate) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (certificateTemplateRepository.existsByNameAndTenantId(name, tenantId)) {
			throw new BusinessException("Certificate template already exists: " + name);
		}
		return certificateTemplateRepository.save(CertificateTemplate.create(name, bodyTemplate));
	}

	@Transactional
	public CertificateTemplate updateDetails(String publicId, String name, String bodyTemplate) {
		CertificateTemplate template = findByPublicId(publicId);
		template.updateDetails(name, bodyTemplate);
		return certificateTemplateRepository.save(template);
	}

	@Transactional
	public CertificateTemplate activate(String publicId) {
		CertificateTemplate template = findByPublicId(publicId);
		template.activate();
		return certificateTemplateRepository.save(template);
	}

	@Transactional
	public CertificateTemplate deactivate(String publicId) {
		CertificateTemplate template = findByPublicId(publicId);
		template.deactivate();
		return certificateTemplateRepository.save(template);
	}
}
