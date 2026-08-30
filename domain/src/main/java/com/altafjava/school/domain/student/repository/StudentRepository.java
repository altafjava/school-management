package com.altafjava.school.domain.student.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

	Page<Student> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<Student> findAllByTenantIdAndEnrollmentStatus(Long tenantId, EnrollmentStatus enrollmentStatus,
			Pageable pageable);

	List<Student> findAllByEnrollmentStatusAndTenantId(EnrollmentStatus enrollmentStatus, Long tenantId);

	List<Student> findAllByIdInAndTenantId(List<Long> ids, Long tenantId);

	long countByEnrollmentStatusAndTenantId(EnrollmentStatus enrollmentStatus, Long tenantId);

	Optional<Student> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByStudentCodeAndTenantId(String studentCode, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	Optional<Student> findByIdAndTenantId(Long id, Long tenantId);

	@Query("SELECT s FROM Student s WHERE s.tenantId = :tenantId AND s.email = :email")
	Optional<Student> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") Long tenantId);

	Optional<Student> findByUserIdAndTenantId(Long userId, Long tenantId);

	/**
	 * Bulk-fetch for {@code SchoolDataRetentionHandler} — inactive students past a tenant's configured retention
	 * window.
	 */
	List<Student> findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(Long tenantId,
			List<EnrollmentStatus> enrollmentStatuses, Instant cutoff);
}
