package com.example.fleet.application.exception;

import java.util.UUID;

/**
 * Thrown by {@link com.example.fleet.application.service.DriverService} when a
 * {@code CreateDriverCommand} references a {@code userId} that does not exist in
 * the {@code UserRepository}.
 * Maps to HTTP 404 in the global exception handler.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }
}
