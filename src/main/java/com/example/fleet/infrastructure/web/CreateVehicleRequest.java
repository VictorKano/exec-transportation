package com.example.fleet.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Web DTO (Data Transfer Object) for the POST /api/v1/vehicles endpoint.
 * Uses Bean Validation annotations to enforce field presence constraints.
 * This provides a first line of defense before the request reaches the application layer.
 *
 * Requirements: 6.1, 6.3
 */
public record CreateVehicleRequest(
        @NotBlank(message = "plate is required")
        String plate,

        @NotBlank(message = "brand is required")
        String brand,

        @NotBlank(message = "model is required")
        String model,

        @NotNull(message = "year is required")
        Integer year,

        UUID driverId   // optional — no validation annotation
) {}
