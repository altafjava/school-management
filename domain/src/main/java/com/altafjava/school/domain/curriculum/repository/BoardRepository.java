package com.altafjava.school.domain.curriculum.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.curriculum.model.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {

	Page<Board> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Board> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);
}
