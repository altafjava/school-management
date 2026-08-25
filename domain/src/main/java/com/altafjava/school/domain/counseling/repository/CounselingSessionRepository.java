package com.altafjava.school.domain.counseling.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.counseling.model.CounselingSession;

public interface CounselingSessionRepository extends JpaRepository<CounselingSession, Long> {

	Page<CounselingSession> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<CounselingSession> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	Optional<CounselingSession> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<CounselingSession> findByIdAndTenantId(Long id, Long tenantId);
}
