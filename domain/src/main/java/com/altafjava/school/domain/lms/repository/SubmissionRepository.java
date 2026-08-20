package com.altafjava.school.domain.lms.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.lms.model.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

	Optional<Submission> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	@Query("SELECT s FROM Submission s WHERE s.assignmentId = :assignmentId AND s.tenantId = :tenantId")
	Page<Submission> findByAssignmentIdAndTenantId(@Param("assignmentId") Long assignmentId,
			@Param("tenantId") Long tenantId, Pageable pageable);

	@Query("SELECT s FROM Submission s WHERE s.assignmentId = :assignmentId AND s.studentId = :studentId "
			+ "AND s.tenantId = :tenantId")
	Optional<Submission> findByAssignmentIdAndStudentIdAndTenantId(@Param("assignmentId") Long assignmentId,
			@Param("studentId") Long studentId, @Param("tenantId") Long tenantId);

	boolean existsByAssignmentIdAndStudentIdAndTenantId(Long assignmentId, Long studentId, Long tenantId);
}
