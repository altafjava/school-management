package com.altafjava.school.domain.counseling.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.counseling.model.CounselingReferral;

public interface CounselingReferralRepository extends JpaRepository<CounselingReferral, Long> {

	Page<CounselingReferral> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<CounselingReferral> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	Optional<CounselingReferral> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
