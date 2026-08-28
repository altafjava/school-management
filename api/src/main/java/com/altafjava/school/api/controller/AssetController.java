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
import com.altafjava.school.api.dto.request.CreateAssetRequest;
import com.altafjava.school.api.dto.request.UpdateAssetLocationRequest;
import com.altafjava.school.api.dto.response.AssetResponse;
import com.altafjava.school.api.mapper.AssetMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.AssetService;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

	private final AssetService assetService;
	private final AssetMapper assetMapper;

	private final SpringDataPageableResolver pageableResolver;

	public AssetController(AssetService assetService, AssetMapper assetMapper,
			SpringDataPageableResolver pageableResolver) {
		this.assetService = assetService;
		this.assetMapper = assetMapper;
		this.pageableResolver = pageableResolver;
	}

	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public Page<AssetResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return assetService.list(pageableResolver.resolve(page, size)).map(assetMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public AssetResponse get(@PathVariable String publicId) {
		return assetMapper.toResponse(assetService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public AssetResponse create(@Valid @RequestBody CreateAssetRequest request) {
		return assetMapper.toResponse(assetService.create(request.assetCode(), request.name(), request.category(),
				request.purchaseDate(), request.purchaseCost(), request.location()));
	}

	@PatchMapping("/{publicId}/location")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public AssetResponse updateLocation(@PathVariable String publicId,
			@Valid @RequestBody UpdateAssetLocationRequest request) {
		return assetMapper.toResponse(assetService.updateLocation(publicId, request.location()));
	}

	@PatchMapping("/{publicId}/maintenance")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public AssetResponse markUnderMaintenance(@PathVariable String publicId) {
		return assetMapper.toResponse(assetService.markUnderMaintenance(publicId));
	}

	@PatchMapping("/{publicId}/available")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public AssetResponse markAvailable(@PathVariable String publicId) {
		return assetMapper.toResponse(assetService.markAvailable(publicId));
	}

	@PatchMapping("/{publicId}/dispose")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSET_MANAGE')")
	public AssetResponse markDisposed(@PathVariable String publicId) {
		return assetMapper.toResponse(assetService.markDisposed(publicId));
	}
}
