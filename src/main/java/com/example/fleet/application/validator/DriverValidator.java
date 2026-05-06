package com.example.fleet.application.validator;

import com.example.fleet.application.command.CreateDriverCommand;
import com.example.fleet.application.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Application-layer validator for {@link CreateDriverCommand}.
 * <p>
 * Throws {@link ValidationException} with a descriptive message for every
 * rule violation. No Spring or JPA dependencies — fully unit-testable without
 * a Spring context.
 * </p>
 *
 * <p>Validation rules (in order):</p>
 * <ol>
 *   <li>userId — non-null</li>
 *   <li>cnh — non-null, non-blank</li>
 *   <li>cnh — alphanumeric only ({@code [A-Za-z0-9]+})</li>
 *   <li>cnh — length 1–20 characters</li>
 *   <li>status — non-null</li>
 * </ol>
 */
public class DriverValidator {

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("[A-Za-z0-9]+");
    private static final int MAX_CNH_LENGTH = 20;

    /**
     * Validates the given {@link CreateDriverCommand}.
     *
     * @param command the command to validate; must not be {@code null}
     * @throws ValidationException if any field fails validation
     */
    public void validate(CreateDriverCommand command) {
        validateUserId(command.userId());
        validateCnh(command.cnh());
        validateStatus(command.status());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void validateUserId(java.util.UUID userId) {
        if (userId == null) {
            throw new ValidationException("userId is required");
        }
    }

    private void validateCnh(String cnh) {
        if (cnh == null || cnh.isBlank()) {
            throw new ValidationException("cnh is required");
        }
        if (!ALPHANUMERIC_PATTERN.matcher(cnh).matches()) {
            throw new ValidationException("cnh must be alphanumeric");
        }
        if (cnh.length() > MAX_CNH_LENGTH) {
            throw new ValidationException("cnh must be between 1 and 20 characters");
        }
    }

    private void validateStatus(com.example.fleet.domain.model.DriverStatus status) {
        if (status == null) {
            throw new ValidationException("status is required");
        }
    }
}
