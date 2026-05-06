package com.example.fleet.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Web DTO (Data Transfer Object) for the POST /api/v1/users endpoint.
 * Uses Bean Validation annotations to enforce field presence and format constraints.
 * This provides a first line of defense before the request reaches the application layer.
 *
 * Requirements: 1.2, 2.1–2.6
 */
public record CreateUserRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email format is invalid")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password,

        @NotBlank(message = "phoneNumber is required")
        String phoneNumber
) {}
