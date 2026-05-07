package com.example.fleet.infrastructure.persistence;

import com.example.fleet.domain.model.Vehicle;
import com.example.fleet.domain.repository.VehicleRepository;
import org.springframework.stereotype.Repository;

/**
 * Infrastructure adapter implementing the domain VehicleRepository interface.
 * Maps between domain Vehicle objects and JPA VehicleJpaEntity objects.
 */
@Repository
public class VehicleRepositoryAdapter implements VehicleRepository {

    private final VehicleJpaRepository jpaRepository;

    public VehicleRepositoryAdapter(VehicleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        // Map domain Vehicle to JPA entity
        VehicleJpaEntity entity = new VehicleJpaEntity(
                vehicle.getId(),
                vehicle.getPlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getDriverId()
        );

        // Persist via JPA repository
        VehicleJpaEntity savedEntity = jpaRepository.save(entity);

        // Map JPA entity back to domain Vehicle
        return new Vehicle(
                savedEntity.getId(),
                savedEntity.getPlate(),
                savedEntity.getBrand(),
                savedEntity.getModel(),
                savedEntity.getYear(),
                savedEntity.getDriverId()
        );
    }

    @Override
    public boolean existsByPlate(String plate) {
        return jpaRepository.existsByPlate(plate);
    }
}
