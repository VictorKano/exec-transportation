package com.example.fleet.application.exception;

/**
 * Thrown by {@link UserService} when a registration attempt uses an email
 * address that is already registered.
 * Maps to HTTP 409 in the global exception handler.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}
