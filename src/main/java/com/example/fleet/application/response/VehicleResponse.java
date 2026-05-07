package com.example.fleet.application.response;

import java.util.UUID;

/**
 * Output value object returned after successful vehicle creation.
 * Never exposes internal persistence details.
 * Plain Java record — no Spring or JPA annotations.
 *
 * <p>{@code driverId} is nullable — null when no driver is assigned to the vehicle.</p>
 */
public record VehicleResponse(
        UUID id,
        String plate,
        String brand,
        String model,
        int year,
        UUID driverId       // null when no driver is assigned
) {}
