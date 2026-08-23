package com.altafjava.school.domain.library.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.library.model.BookCopy;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

	Page<BookCopy> findAllByBookIdAndTenantId(Long bookId, Long tenantId, Pageable pageable);

	List<BookCopy> findAllByBookIdAndTenantId(Long bookId, Long tenantId);

	Optional<BookCopy> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<BookCopy> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByBookIdAndCopyCodeAndTenantId(Long bookId, String copyCode, Long tenantId);
}
