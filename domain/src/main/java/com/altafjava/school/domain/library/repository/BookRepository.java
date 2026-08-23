package com.altafjava.school.domain.library.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.library.model.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

	Page<Book> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Book> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
