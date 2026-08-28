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
import com.altafjava.school.api.dto.request.AddRouteStopRequest;
import com.altafjava.school.api.dto.request.CreateRouteRequest;
import com.altafjava.school.api.dto.request.UpdateRouteRequest;
import com.altafjava.school.api.dto.response.RouteResponse;
import com.altafjava.school.api.dto.response.RouteStopResponse;
import com.altafjava.school.api.mapper.RouteMapper;
import com.altafjava.school.api.mapper.RouteStopMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.RouteService;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

	private final RouteService routeService;
	private final RouteMapper routeMapper;
	private final RouteStopMapper routeStopMapper;

	private final SpringDataPageableResolver pageableResolver;

	public RouteController(RouteService routeService, RouteMapper routeMapper, RouteStopMapper routeStopMapper,
			SpringDataPageableResolver pageableResolver) {
		this.routeService = routeService;
		this.routeMapper = routeMapper;
		this.routeStopMapper = routeStopMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TRANSPORT_ROUTE_READ')")
	public Page<RouteResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return routeService.list(pageableResolver.resolve(page, size)).map(routeMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TRANSPORT_ROUTE_READ')")
	public RouteResponse get(@PathVariable String publicId) {
		return routeMapper.toResponse(routeService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TRANSPORT_ROUTE_WRITE')")
	public RouteResponse create(@Valid @RequestBody CreateRouteRequest request) {
		return routeMapper.toResponse(routeService.create(request.name(), request.code(), request.description()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TRANSPORT_ROUTE_WRITE')")
	public RouteResponse updateDetails(@PathVariable String publicId, @Valid @RequestBody UpdateRouteRequest request) {
		return routeMapper.toResponse(
				routeService.updateDetails(publicId, request.name(), request.code(), request.description()));
	}

	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TRANSPORT_ROUTE_WRITE')")
	public RouteResponse deactivate(@PathVariable String publicId) {
		return routeMapper.toResponse(routeService.deactivate(publicId));
	}

	@GetMapping("/{publicId}/stops")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TRANSPORT_ROUTE_READ')")
	public List<RouteStopResponse> listStops(@PathVariable String publicId) {
		return routeService.listStops(publicId).stream().map(routeStopMapper::toResponse).toList();
	}

	@PostMapping("/{publicId}/stops")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TRANSPORT_ROUTE_WRITE')")
	public RouteStopResponse addStop(@PathVariable String publicId, @Valid @RequestBody AddRouteStopRequest request) {
		return routeStopMapper.toResponse(routeService.addStop(publicId, request.stopName(), request.sequenceOrder(),
				request.pickupTime(), request.dropTime()));
	}
}
