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
import com.altafjava.school.api.dto.request.CreateEventRequest;
import com.altafjava.school.api.dto.request.RegisterForEventRequest;
import com.altafjava.school.api.dto.request.UpdateEventRequest;
import com.altafjava.school.api.dto.response.EventRegistrationResponse;
import com.altafjava.school.api.dto.response.EventResponse;
import com.altafjava.school.api.mapper.EventMapper;
import com.altafjava.school.api.mapper.EventRegistrationMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.EventRegistrationService;
import com.altafjava.school.application.service.EventService;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

	private final EventService eventService;
	private final EventMapper eventMapper;
	private final EventRegistrationService eventRegistrationService;
	private final EventRegistrationMapper eventRegistrationMapper;

	private final SpringDataPageableResolver pageableResolver;

	public EventController(EventService eventService, EventMapper eventMapper,
			EventRegistrationService eventRegistrationService, EventRegistrationMapper eventRegistrationMapper,
			SpringDataPageableResolver pageableResolver) {
		this.eventService = eventService;
		this.eventMapper = eventMapper;
		this.eventRegistrationService = eventRegistrationService;
		this.eventRegistrationMapper = eventRegistrationMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public Page<EventResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return eventService.list(pageableResolver.resolve(page, size)).map(eventMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT)
	public EventResponse get(@PathVariable String publicId) {
		return eventMapper.toResponse(eventService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public EventResponse create(@Valid @RequestBody CreateEventRequest request) {
		return eventMapper.toResponse(eventService.create(request.title(), request.description(),
				request.eventDate(), request.location(), request.registrationRequired(), request.capacity()));
	}

	@PatchMapping("/{publicId}")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public EventResponse updateDetails(@PathVariable String publicId, @Valid @RequestBody UpdateEventRequest request) {
		return eventMapper.toResponse(eventService.updateDetails(publicId, request.title(), request.description(),
				request.eventDate(), request.location()));
	}

	@PatchMapping("/{publicId}/cancel")
	@PreAuthorize(Roles.HAS_TENANT_ADMIN)
	public EventResponse cancel(@PathVariable String publicId) {
		return eventMapper.toResponse(eventService.cancel(publicId));
	}

	@GetMapping("/{publicId}/registrations")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public Page<EventRegistrationResponse> listRegistrations(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return eventRegistrationService.listForEvent(publicId, pageableResolver.resolve(page, size))
				.map(eventRegistrationMapper::toResponse);
	}

	@PostMapping("/{publicId}/registrations")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_PARENT_OR_STUDENT)
	public EventRegistrationResponse register(@PathVariable String publicId,
			@Valid @RequestBody RegisterForEventRequest request) {
		return eventRegistrationMapper.toResponse(
				eventRegistrationService.register(publicId, request.studentPublicId()));
	}

	@PatchMapping("/registrations/{registrationPublicId}/cancel")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_PARENT_OR_STUDENT)
	public EventRegistrationResponse cancelRegistration(@PathVariable String registrationPublicId) {
		return eventRegistrationMapper.toResponse(eventRegistrationService.cancel(registrationPublicId));
	}

	@PatchMapping("/registrations/{registrationPublicId}/attended")
	@PreAuthorize(SchoolRoles.HAS_TENANT_ADMIN_OR_TEACHER)
	public EventRegistrationResponse markAttended(@PathVariable String registrationPublicId) {
		return eventRegistrationMapper.toResponse(eventRegistrationService.markAttended(registrationPublicId));
	}
}
