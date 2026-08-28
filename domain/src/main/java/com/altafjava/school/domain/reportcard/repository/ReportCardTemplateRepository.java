package com.altafjava.school.domain.reportcard.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.reportcard.model.ReportCardTemplate;

public interface ReportCardTemplateRepository extends JpaRepository<ReportCardTemplate, Long> {

	Optional<ReportCardTemplate> findByTenantId(Long tenantId);
}
