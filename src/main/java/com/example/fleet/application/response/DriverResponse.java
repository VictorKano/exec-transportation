package com.example.fleet.application.response;

import com.example.fleet.domain.model.DriverStatus;

import java.util.UUID;

/**
 * Output value object returned after successful driver creation.
 * Never exposes internal persistence details.
 * Plain Java record — no Spring or JPA annotations.
 */
public record DriverResponse(
        UUID id,
        UUID userId,
        String cnh,
        DriverStatus status
) {}
