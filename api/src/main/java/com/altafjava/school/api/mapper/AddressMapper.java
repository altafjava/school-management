package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.request.AddressRequest;
import com.altafjava.school.api.dto.response.AddressResponse;
import com.altafjava.school.domain.common.model.Address;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AddressMapper {

	Address toDomain(AddressRequest request);

	AddressResponse toResponse(Address address);
}
