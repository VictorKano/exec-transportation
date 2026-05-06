package com.example.fleet.infrastructure.web;

import com.example.fleet.domain.model.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Web DTO (Data Transfer Object) for the POST /api/v1/drivers endpoint.
 * Uses Bean Validation annotations to enforce field presence and format constraints.
 * This provides a first line of defense before the request reaches the application layer.
 *
 * Note: CNH is PII under LGPD — must never appear in logs or error response bodies.
 *
 * Requirements: 9.1
 */
public record CreateDriverRequest(
        @NotNull(message = "userId is required")
        UUID userId,

        @NotBlank(message = "cnh is required")
        @Size(min = 1, max = 20, message = "cnh must be between 1 and 20 characters")
        @Pattern(regexp = "[A-Za-z0-9]+", message = "cnh must be alphanumeric")
        String cnh,

        @NotNull(message = "status is required")
        DriverStatus status
) {}
