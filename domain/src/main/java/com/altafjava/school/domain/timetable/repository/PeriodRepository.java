package com.altafjava.school.domain.timetable.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.timetable.model.Period;

public interface PeriodRepository extends JpaRepository<Period, Long> {

	Page<Period> findAllByTenantId(Long tenantId, Pageable pageable);

	List<Period> findAllByTenantIdOrderByDisplayOrderAsc(Long tenantId);

	Optional<Period> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);
}
