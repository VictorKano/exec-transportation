package com.example.fleet.application.exception;

/**
 * Application-layer alias for {@link com.example.fleet.domain.exception.InvalidTokenException}.
 *
 * <p>Kept here for backward compatibility. Prefer referencing the domain exception directly
 * in new code.</p>
 */
public class InvalidTokenException extends com.example.fleet.domain.exception.InvalidTokenException {

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
