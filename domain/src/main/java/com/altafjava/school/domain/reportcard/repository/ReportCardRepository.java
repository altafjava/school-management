package com.altafjava.school.domain.reportcard.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.reportcard.model.ReportCard;

public interface ReportCardRepository extends JpaRepository<ReportCard, Long> {

	Page<ReportCard> findByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	Optional<ReportCard> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByStudentIdAndTermIdAndTenantId(Long studentId, Long termId, Long tenantId);

	Optional<ReportCard> findByStudentIdAndTermIdAndTenantId(Long studentId, Long termId, Long tenantId);
}
