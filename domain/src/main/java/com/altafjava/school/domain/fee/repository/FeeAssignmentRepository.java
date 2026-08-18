package com.altafjava.school.domain.fee.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.fee.model.FeeAssignment;

public interface FeeAssignmentRepository extends JpaRepository<FeeAssignment, Long> {

	Optional<FeeAssignment> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByFeeStructureIdAndStudentIdAndTenantId(Long feeStructureId, Long studentId, Long tenantId);

	boolean existsByFeeStructureIdAndClassroomIdAndTenantId(Long feeStructureId, Long classroomId, Long tenantId);

	@Query("SELECT a FROM FeeAssignment a WHERE a.tenantId = :tenantId AND a.studentId = :studentId")
	List<FeeAssignment> findByTenantIdAndStudentId(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId);

	@Query("SELECT a FROM FeeAssignment a WHERE a.tenantId = :tenantId AND a.classroomId = :classroomId")
	List<FeeAssignment> findByTenantIdAndClassroomId(@Param("tenantId") Long tenantId,
			@Param("classroomId") Long classroomId);

	@Query("SELECT a FROM FeeAssignment a WHERE a.tenantId = :tenantId AND a.feeStructureId = :feeStructureId")
	Page<FeeAssignment> findByFeeStructureIdAndTenantId(@Param("tenantId") Long tenantId,
			@Param("feeStructureId") Long feeStructureId, Pageable pageable);
}
