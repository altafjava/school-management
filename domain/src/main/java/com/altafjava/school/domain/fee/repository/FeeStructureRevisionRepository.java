package com.altafjava.school.domain.fee.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.fee.model.FeeStructureRevision;

public interface FeeStructureRevisionRepository extends JpaRepository<FeeStructureRevision, Long> {

	@Query("SELECT r FROM FeeStructureRevision r WHERE r.tenantId = :tenantId AND r.feeStructureId = :feeStructureId "
			+ "ORDER BY r.createdAt DESC")
	Page<FeeStructureRevision> findByFeeStructureIdAndTenantId(@Param("tenantId") Long tenantId,
			@Param("feeStructureId") Long feeStructureId, Pageable pageable);
}
