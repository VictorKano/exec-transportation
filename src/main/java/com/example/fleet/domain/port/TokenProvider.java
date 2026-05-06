package com.example.fleet.domain.port;

import com.example.fleet.domain.exception.InvalidTokenException;
import com.example.fleet.domain.model.Claims;

import java.util.UUID;

/**
 * Domain interface for JWT generation and validation.
 * No Spring, JJWT, or framework imports — implementations live in the infrastructure layer.
 */
public interface TokenProvider {

    /**
     * Generates a signed JWT for the given user identity.
     *
     * @param userId the user's UUID (becomes the "sub" claim)
     * @param email  the user's email (stored as a custom claim)
     * @return a compact, signed JWT string
     */
    String generate(UUID userId, String email);

    /**
     * Validates the given JWT and returns its claims.
     *
     * @param token the compact JWT string to validate
     * @return the parsed {@link Claims}
     * @throws InvalidTokenException if the token is expired, has an invalid signature, or is malformed
     */
    Claims validate(String token);
}
