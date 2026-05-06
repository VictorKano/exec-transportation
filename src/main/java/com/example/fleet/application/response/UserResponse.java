package com.example.fleet.application.response;

import java.util.UUID;

/**
 * Output value object returned after successful user creation.
 * Never includes the password (plain-text or hashed).
 * Plain Java record — no Spring or JPA annotations.
 */
public record UserResponse(
        UUID id,
        String name,
        String email,
        String phoneNumber
) {}
