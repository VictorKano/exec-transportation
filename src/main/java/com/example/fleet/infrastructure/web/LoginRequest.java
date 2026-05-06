package com.example.fleet.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Web DTO (Data Transfer Object) for the POST /api/v1/auth/login endpoint.
 * Uses Bean Validation annotations to enforce field presence before the request
 * reaches the application layer.
 *
 * Requirements: 1.1, 1.2, 2.1, 2.2
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {}
