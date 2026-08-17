package com.altafjava.school.domain.classroom.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.classroom.model.Classroom;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

	Page<Classroom> findAllByTenantId(Long tenantId, Pageable pageable);

	List<Classroom> findAllByTenantId(Long tenantId);

	Optional<Classroom> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByClassTeacherIdAndTenantId(Long classTeacherId, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	List<Classroom> findAllByClassTeacherIdAndTenantId(Long classTeacherId, Long tenantId);
}
