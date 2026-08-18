package com.altafjava.school.domain.classroom.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;

public interface StudentClassroomLinkRepository extends JpaRepository<StudentClassroomLink, Long> {

	Optional<StudentClassroomLink> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByStudentIdAndAcademicYearIdAndTenantId(Long studentId, Long academicYearId, Long tenantId);

	@Query("SELECT l FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.studentId = :studentId "
			+ "AND l.classroomId = :classroomId")
	Optional<StudentClassroomLink> findByStudentIdAndClassroomId(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId, @Param("classroomId") Long classroomId);

	@Query("SELECT l FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.studentId = :studentId "
			+ "AND l.academicYearId = :academicYearId")
	Optional<StudentClassroomLink> findByStudentIdAndAcademicYearId(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId, @Param("academicYearId") Long academicYearId);

	@Query("SELECT l FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.classroomId = :classroomId")
	Page<StudentClassroomLink> findByClassroomId(@Param("tenantId") Long tenantId,
			@Param("classroomId") Long classroomId, Pageable pageable);

	@Query("SELECT l FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.studentId = :studentId")
	List<StudentClassroomLink> findByStudentId(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId);
}
