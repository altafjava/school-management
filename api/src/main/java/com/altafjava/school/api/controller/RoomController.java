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
import com.altafjava.school.api.dto.request.CreateRoomRequest;
import com.altafjava.school.api.dto.request.UpdateRoomRequest;
import com.altafjava.school.api.dto.response.RoomResponse;
import com.altafjava.school.api.mapper.RoomMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.RoomService;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

	private final RoomService roomService;
	private final RoomMapper roomMapper;

	private final SpringDataPageableResolver pageableResolver;

	public RoomController(RoomService roomService, RoomMapper roomMapper,
			SpringDataPageableResolver pageableResolver) {
		this.roomService = roomService;
		this.roomMapper = roomMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<RoomResponse> listForBuilding(
			@RequestParam String hostelBuildingPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return roomService.listForBuilding(hostelBuildingPublicId, pageableResolver.resolve(page, size))
				.map(roomMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public RoomResponse get(@PathVariable String publicId) {
		return roomMapper.toResponse(roomService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public RoomResponse create(
			@RequestParam String hostelBuildingPublicId,
			@Valid @RequestBody CreateRoomRequest request) {
		return roomMapper.toResponse(
				roomService.create(hostelBuildingPublicId, request.roomNumber(), request.capacity()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public RoomResponse updateDetails(@PathVariable String publicId, @Valid @RequestBody UpdateRoomRequest request) {
		return roomMapper.toResponse(roomService.updateDetails(publicId, request.roomNumber(), request.capacity()));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public RoomResponse deactivate(@PathVariable String publicId) {
		return roomMapper.toResponse(roomService.deactivate(publicId));
	}
}
