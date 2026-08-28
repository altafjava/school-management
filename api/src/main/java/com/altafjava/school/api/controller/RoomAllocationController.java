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
import com.altafjava.school.api.dto.request.AllocateRoomRequest;
import com.altafjava.school.api.dto.request.VacateRoomAllocationRequest;
import com.altafjava.school.api.dto.response.RoomAllocationResponse;
import com.altafjava.school.api.mapper.RoomAllocationMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.RoomAllocationService;

@RestController
@RequestMapping("/api/v1/room-allocations")
public class RoomAllocationController {

	private final RoomAllocationService roomAllocationService;
	private final RoomAllocationMapper roomAllocationMapper;

	private final SpringDataPageableResolver pageableResolver;

	public RoomAllocationController(RoomAllocationService roomAllocationService,
			RoomAllocationMapper roomAllocationMapper, SpringDataPageableResolver pageableResolver) {
		this.roomAllocationService = roomAllocationService;
		this.roomAllocationMapper = roomAllocationMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ROOM_ALLOCATION_READ')")
	public Page<RoomAllocationResponse> listForRoom(
			@RequestParam String roomPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return roomAllocationService.listForRoom(roomPublicId, pageableResolver.resolve(page, size))
				.map(roomAllocationMapper::toResponse);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ROOM_ALLOCATION_WRITE')")
	public RoomAllocationResponse allocate(@Valid @RequestBody AllocateRoomRequest request) {
		return roomAllocationMapper.toResponse(roomAllocationService.allocate(request.studentPublicId(),
				request.roomPublicId(), request.allocatedFrom()));
	}

	@PatchMapping("/{publicId}/vacate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ROOM_ALLOCATION_WRITE')")
	public RoomAllocationResponse vacate(@PathVariable String publicId,
			@Valid @RequestBody VacateRoomAllocationRequest request) {
		return roomAllocationMapper.toResponse(roomAllocationService.vacate(publicId, request.allocatedUntil()));
	}
}
