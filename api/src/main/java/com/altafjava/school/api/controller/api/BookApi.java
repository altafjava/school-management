package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AddBookCopyRequest;
import com.altafjava.school.api.dto.request.CreateBookRequest;
import com.altafjava.school.api.dto.response.BookCopyResponse;
import com.altafjava.school.api.dto.response.BookResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Book", description = "APIs for managing Book operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface BookApi {

	@Operation(summary = "List", operationId = "book_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<BookResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "book_get")
	public ApiResponse<BookResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", operationId = "book_create")
	public ApiResponse<BookResponse> create(@Valid @RequestBody CreateBookRequest request);

	@Operation(summary = "Deactivate", operationId = "book_deactivate")
	public ApiResponse<BookResponse> deactivate(@PathVariable String publicId);

	@Operation(summary = "List copies", operationId = "book_listCopies")
	public ApiResponse<List<BookCopyResponse>> listCopies(@PathVariable String publicId);

	@Operation(summary = "Add copy", operationId = "book_addCopy")
	public ApiResponse<BookCopyResponse> addCopy(@PathVariable String publicId,
			@Valid @RequestBody AddBookCopyRequest request);

	@Operation(summary = "Mark copy lost", operationId = "book_markCopyLost")
	public ApiResponse<BookCopyResponse> markCopyLost(@PathVariable String copyPublicId);

	@Operation(summary = "Mark copy damaged", operationId = "book_markCopyDamaged")
	public ApiResponse<BookCopyResponse> markCopyDamaged(@PathVariable String copyPublicId);
}
