package com.altafjava.school.api.controller;

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
import com.altafjava.school.api.dto.request.CreateBoardRequest;
import com.altafjava.school.api.dto.request.UpdateBoardRequest;
import com.altafjava.school.api.dto.response.BoardResponse;
import com.altafjava.school.api.mapper.BoardMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.BoardService;

@RestController
@RequestMapping("/api/v1/boards")
public class BoardController {

	private final BoardService boardService;
	private final BoardMapper boardMapper;

	private final SpringDataPageableResolver pageableResolver;

	public BoardController(BoardService boardService, BoardMapper boardMapper,
			SpringDataPageableResolver pageableResolver) {
		this.boardService = boardService;
		this.boardMapper = boardMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<BoardResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return boardService.list(pageableResolver.resolve(page, size)).map(boardMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public BoardResponse get(@PathVariable String publicId) {
		return boardMapper.toResponse(boardService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public BoardResponse create(@Valid @RequestBody CreateBoardRequest request) {
		return boardMapper.toResponse(boardService.create(request.name(), request.code(), request.description()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public BoardResponse updateDetails(@PathVariable String publicId, @Valid @RequestBody UpdateBoardRequest request) {
		return boardMapper.toResponse(
				boardService.updateDetails(publicId, request.name(), request.code(), request.description()));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public BoardResponse deactivate(@PathVariable String publicId) {
		return boardMapper.toResponse(boardService.deactivate(publicId));
	}
}
