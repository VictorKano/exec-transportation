package com.example.fleet.application.response;

import java.util.UUID;

/**
 * Output value object returned after a successful login.
 * Contains the signed JWT and the authenticated user's identity.
 * Never includes the password (plain-text or hashed).
 * Plain Java record — no Spring or JPA annotations.
 */
public record AuthResponse(String token, UUID userId, String email) {}
