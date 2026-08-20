package com.altafjava.school.domain.lms.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.lms.model.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

	Page<Lesson> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Lesson> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Lesson> findByIdAndTenantId(Long id, Long tenantId);

	@Query("SELECT l FROM Lesson l WHERE l.classroomId = :classroomId AND l.tenantId = :tenantId")
	Page<Lesson> findByClassroomIdAndTenantId(@Param("classroomId") Long classroomId,
			@Param("tenantId") Long tenantId, Pageable pageable);
}
