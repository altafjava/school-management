package com.altafjava.school.domain.curriculum.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.curriculum.model.Curriculum;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

	Page<Curriculum> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Curriculum> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Curriculum> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);
}
