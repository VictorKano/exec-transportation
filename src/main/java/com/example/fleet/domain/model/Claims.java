package com.example.fleet.domain.model;

import java.util.UUID;

/**
 * Domain value object representing the claims extracted from a validated JWT.
 * Plain Java record — no Spring, JJWT, or any framework imports.
 *
 * @param userId the user's unique identifier (mapped from the JWT "sub" claim)
 * @param email  the user's email address (mapped from the JWT "email" claim)
 */
public record Claims(UUID userId, String email) {
}
