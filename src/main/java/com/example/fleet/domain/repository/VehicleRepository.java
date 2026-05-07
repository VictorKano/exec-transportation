package com.example.fleet.domain.repository;

import com.example.fleet.domain.model.Vehicle;

/**
 * Domain repository interface for Vehicle persistence.
 * No Spring, JPA, or framework annotations — implementations live in the infrastructure layer.
 */
public interface VehicleRepository {

    /**
     * Persists the given vehicle and returns the saved instance.
     *
     * @param vehicle the vehicle to save
     * @return the saved vehicle
     */
    Vehicle save(Vehicle vehicle);

    /**
     * Returns {@code true} if a vehicle with the given plate already exists.
     *
     * @param plate the license plate to check
     * @return {@code true} if the plate is already registered, {@code false} otherwise
     */
    boolean existsByPlate(String plate);
}
