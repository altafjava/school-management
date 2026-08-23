package com.altafjava.school.domain.library.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.library.model.Circulation;

public interface CirculationRepository extends JpaRepository<Circulation, Long> {

	Page<Circulation> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	Optional<Circulation> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Circulation> findByBookCopyIdAndTenantIdAndReturnedAtIsNull(Long bookCopyId, Long tenantId);

	List<Circulation> findAllByTenantIdAndReturnedAtIsNull(Long tenantId);
}
