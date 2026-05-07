package com.example.fleet.application.exception;

import java.util.UUID;

/**
 * Thrown by {@link com.example.fleet.application.service.VehicleService} when a
 * {@code CreateVehicleCommand} references a {@code driverId} that does not exist
 * in the {@code DriverRepository}.
 * Maps to HTTP 404 in the global exception handler.
 */
public class DriverNotFoundException extends RuntimeException {

    public DriverNotFoundException(UUID driverId) {
        super("Driver not found: " + driverId);
    }
}
