package com.example.fleet.application.exception;

/**
 * Thrown by the application layer when input validation fails.
 * Maps to HTTP 400 in the global exception handler.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
