package com.altafjava.school.domain.transport.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.transport.model.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

	Page<Vehicle> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Vehicle> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Vehicle> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByRegistrationNumberAndTenantId(String registrationNumber, Long tenantId);
}
