package com.altafjava.school.domain.term.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.term.model.Term;

public interface TermRepository extends JpaRepository<Term, Long> {

	Page<Term> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Term> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByNameAndAcademicYearIdAndTenantId(String name, Long academicYearId, Long tenantId);
}
