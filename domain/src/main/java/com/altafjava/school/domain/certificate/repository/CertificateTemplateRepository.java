package com.altafjava.school.domain.certificate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.certificate.model.CertificateTemplate;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {

	Page<CertificateTemplate> findAllByTenantId(Long tenantId, Pageable pageable);

	List<CertificateTemplate> findAllByTenantIdAndActiveTrue(Long tenantId);

	Optional<CertificateTemplate> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<CertificateTemplate> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);
}
