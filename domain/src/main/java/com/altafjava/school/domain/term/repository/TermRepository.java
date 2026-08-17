package com.altafjava.school.domain.term.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.term.model.Term;

public interface TermRepository extends JpaRepository<Term, Long> {

	Page<Term> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Term> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByNameAndAcademicYearIdAndTenantId(String name, Long academicYearId, Long tenantId);

	Optional<Term> findByIdAndTenantId(Long id, Long tenantId);

	// Terms have no isCurrent flag (unlike AcademicYear) — "the current term" is derived from
	// whether today falls within its date range.
	@Query("SELECT t FROM Term t WHERE t.tenantId = :tenantId AND :today BETWEEN t.startDate AND t.endDate")
	Optional<Term> findCurrentByTenantId(@Param("tenantId") Long tenantId, @Param("today") LocalDate today);
}
