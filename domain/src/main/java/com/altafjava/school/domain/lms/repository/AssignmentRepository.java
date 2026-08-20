package com.altafjava.school.domain.lms.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.lms.model.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

	Page<Assignment> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Assignment> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Assignment> findByIdAndTenantId(Long id, Long tenantId);

	@Query("SELECT a FROM Assignment a WHERE a.classroomId = :classroomId AND a.tenantId = :tenantId")
	Page<Assignment> findByClassroomIdAndTenantId(@Param("classroomId") Long classroomId,
			@Param("tenantId") Long tenantId, Pageable pageable);
}
