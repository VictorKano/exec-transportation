package com.example.fleet.domain.port;

/**
 * Domain interface for password encoding.
 * No Spring, JPA, or framework annotations — implementations live in the infrastructure layer.
 */
public interface PasswordEncoder {

    /**
     * Encodes the given raw password and returns the hashed result.
     *
     * @param rawPassword the plain-text password to encode
     * @return the hashed password
     */
    String encode(String rawPassword);

    /**
     * Verifies that a raw (plain-text) password matches an encoded (hashed) password.
     *
     * @param rawPassword     the plain-text password to check
     * @param encodedPassword the previously encoded password to compare against
     * @return {@code true} if the raw password matches the encoded password, {@code false} otherwise
     */
    boolean matches(String rawPassword, String encodedPassword);
}
