package com.altafjava.school.domain.certificate.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.certificate.model.CertificateIssuance;

public interface CertificateIssuanceRepository extends JpaRepository<CertificateIssuance, Long> {

	@Query("SELECT c FROM CertificateIssuance c WHERE c.tenantId = :tenantId AND c.studentId = :studentId")
	Page<CertificateIssuance> findByStudentIdAndTenantId(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId, Pageable pageable);

	Optional<CertificateIssuance> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	// Verification is scoped by tenant (resolved via the X-Tenant-ID header, the same as every
	// other endpoint in this codebase — see AdmissionController#apply for the precedent of a
	// permitAll endpoint that still requires tenant resolution) plus the code itself.
	Optional<CertificateIssuance> findByVerificationCodeAndTenantId(String verificationCode, Long tenantId);

	boolean existsByVerificationCodeAndTenantId(String verificationCode, Long tenantId);
}
