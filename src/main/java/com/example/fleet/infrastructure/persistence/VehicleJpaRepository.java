package com.example.fleet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository interface for VehicleJpaEntity.
 * Infrastructure layer — extends Spring Data JpaRepository.
 */
public interface VehicleJpaRepository extends JpaRepository<VehicleJpaEntity, UUID> {

    /**
     * Derived query method to check if a vehicle with the given plate already exists.
     * Checks existence without loading the full entity.
     *
     * @param plate the license plate to check
     * @return {@code true} if a vehicle with the plate exists, {@code false} otherwise
     */
    boolean existsByPlate(String plate);
}
