package com.example.fleet.application.command;

import com.example.fleet.domain.model.DriverStatus;

import java.util.UUID;

/**
 * Input value object (command) for creating a new driver profile.
 * Plain Java record — no Spring or JPA annotations.
 */
public record CreateDriverCommand(
        UUID userId,
        String cnh,
        DriverStatus status
) {}
