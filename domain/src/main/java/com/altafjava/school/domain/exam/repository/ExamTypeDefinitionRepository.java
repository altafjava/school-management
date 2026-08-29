package com.altafjava.school.domain.exam.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;

public interface ExamTypeDefinitionRepository extends JpaRepository<ExamTypeDefinition, Long> {

	List<ExamTypeDefinition> findAllByTenantIdOrderByDisplayOrder(Long tenantId);

	List<ExamTypeDefinition> findAllByTenantIdAndActiveTrueOrderByDisplayOrder(Long tenantId);

	Optional<ExamTypeDefinition> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<ExamTypeDefinition> findByCodeAndTenantId(String code, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);
}
