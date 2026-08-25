package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.dto.request.AddBookCopyRequest;
import com.altafjava.school.api.dto.request.CreateBookRequest;
import com.altafjava.school.api.dto.response.BookCopyResponse;
import com.altafjava.school.api.dto.response.BookResponse;
import com.altafjava.school.api.mapper.BookCopyMapper;
import com.altafjava.school.api.mapper.BookMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.BookCatalogService;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

	private final BookCatalogService bookCatalogService;
	private final BookMapper bookMapper;
	private final BookCopyMapper bookCopyMapper;

	private final SpringDataPageableResolver pageableResolver;

	public BookController(BookCatalogService bookCatalogService, BookMapper bookMapper,
			BookCopyMapper bookCopyMapper, SpringDataPageableResolver pageableResolver) {
		this.bookCatalogService = bookCatalogService;
		this.bookMapper = bookMapper;
		this.bookCopyMapper = bookCopyMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public Page<BookResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return bookCatalogService.listBooks(pageableResolver.resolve(page, size)).map(bookMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public BookResponse get(@PathVariable String publicId) {
		return bookMapper.toResponse(bookCatalogService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public BookResponse create(@Valid @RequestBody CreateBookRequest request) {
		return bookMapper.toResponse(bookCatalogService.createBook(request.isbn(), request.title(), request.author(),
				request.publisher(), request.category()));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public BookResponse deactivate(@PathVariable String publicId) {
		return bookMapper.toResponse(bookCatalogService.deactivateBook(publicId));
	}

	@GetMapping("/{publicId}/copies")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public List<BookCopyResponse> listCopies(@PathVariable String publicId) {
		return bookCatalogService.listCopies(publicId).stream().map(bookCopyMapper::toResponse).toList();
	}

	@PostMapping("/{publicId}/copies")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public BookCopyResponse addCopy(@PathVariable String publicId, @Valid @RequestBody AddBookCopyRequest request) {
		return bookCopyMapper.toResponse(bookCatalogService.addCopy(publicId, request.copyCode()));
	}

	@PatchMapping("/copies/{copyPublicId}/lost")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public BookCopyResponse markCopyLost(@PathVariable String copyPublicId) {
		return bookCopyMapper.toResponse(bookCatalogService.markCopyLost(copyPublicId));
	}

	@PatchMapping("/copies/{copyPublicId}/damaged")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public BookCopyResponse markCopyDamaged(@PathVariable String copyPublicId) {
		return bookCopyMapper.toResponse(bookCatalogService.markCopyDamaged(copyPublicId));
	}
}
