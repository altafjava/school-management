package com.altafjava.school.domain.subject.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.subject.model.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

	Page<Subject> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Subject> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Subject> findByIdAndTenantId(Long id, Long tenantId);

	List<Subject> findAllByIdInAndTenantId(List<Long> ids, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);
}
