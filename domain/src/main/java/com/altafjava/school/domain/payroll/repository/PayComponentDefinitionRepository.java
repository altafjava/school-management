package com.altafjava.school.domain.payroll.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.payroll.model.PayComponentDefinition;

public interface PayComponentDefinitionRepository extends JpaRepository<PayComponentDefinition, Long> {

	List<PayComponentDefinition> findAllByTenantIdOrderByDisplayOrder(Long tenantId);

	List<PayComponentDefinition> findAllByTenantIdAndActiveTrueOrderByDisplayOrder(Long tenantId);

	Optional<PayComponentDefinition> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<PayComponentDefinition> findByCodeAndTenantId(String code, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);
}
