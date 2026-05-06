package com.example.fleet.domain.exception;

/**
 * Thrown when a JWT token is invalid, expired, or malformed.
 * Defined in the domain layer so that the {@code TokenProvider} interface can reference it
 * without creating a domain → application dependency.
 *
 * <p>Maps to HTTP 500 in the current scope; a future JWT filter will return HTTP 401
 * for protected endpoints.</p>
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
