package com.altafjava.school.domain.holiday.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.holiday.model.Holiday;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

	Page<Holiday> findAllByTenantId(Long tenantId, Pageable pageable);

	List<Holiday> findAllByTenantId(Long tenantId);

	Optional<Holiday> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
