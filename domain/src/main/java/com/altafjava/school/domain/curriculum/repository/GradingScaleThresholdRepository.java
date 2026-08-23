package com.altafjava.school.domain.curriculum.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;

public interface GradingScaleThresholdRepository extends JpaRepository<GradingScaleThreshold, Long> {

	List<GradingScaleThreshold> findAllByGradingScaleIdAndTenantId(Long gradingScaleId, Long tenantId);
}
