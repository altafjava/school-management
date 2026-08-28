package com.altafjava.school.domain.grade.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.grade.model.GradeCorrection;

public interface GradeCorrectionRepository extends JpaRepository<GradeCorrection, Long> {

	@Query("SELECT c FROM GradeCorrection c WHERE c.tenantId = :tenantId AND c.gradeId = :gradeId "
			+ "ORDER BY c.createdAt DESC")
	Page<GradeCorrection> findByGradeIdAndTenantId(@Param("tenantId") Long tenantId,
			@Param("gradeId") Long gradeId, Pageable pageable);
}
