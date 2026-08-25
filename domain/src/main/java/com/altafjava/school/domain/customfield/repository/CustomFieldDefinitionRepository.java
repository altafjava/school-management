package com.altafjava.school.domain.customfield.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.customfield.model.CustomFieldDefinition;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;

public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, Long> {

	Page<CustomFieldDefinition> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<CustomFieldDefinition> findAllByTenantIdAndEntityType(Long tenantId, CustomFieldEntityType entityType,
			Pageable pageable);

	List<CustomFieldDefinition> findAllByTenantIdAndEntityTypeAndActiveTrue(Long tenantId,
			CustomFieldEntityType entityType);

	Optional<CustomFieldDefinition> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<CustomFieldDefinition> findByTenantIdAndEntityTypeAndFieldKey(Long tenantId,
			CustomFieldEntityType entityType, String fieldKey);

	boolean existsByTenantIdAndEntityTypeAndFieldKey(Long tenantId, CustomFieldEntityType entityType,
			String fieldKey);
}
