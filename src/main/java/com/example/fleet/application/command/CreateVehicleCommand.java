package com.example.fleet.application.command;

import java.util.UUID;

/**
 * Input value object (command) for creating a new vehicle.
 * Plain Java record — no Spring or JPA annotations.
 *
 * <p>{@code year} is {@link Integer} (boxed) so {@code VehicleValidator} can detect null.</p>
 * <p>{@code driverId} is nullable — null means no driver assignment at creation time.</p>
 */
public record CreateVehicleCommand(
        String plate,
        String brand,
        String model,
        Integer year,       // Integer (nullable) so VehicleValidator can detect null
        UUID driverId       // nullable — null means no driver assignment
) {}
