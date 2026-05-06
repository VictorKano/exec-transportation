package com.example.fleet.application.validator;

import com.example.fleet.application.command.CreateUserCommand;
import com.example.fleet.application.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Application-layer validator for {@link CreateUserCommand}.
 * <p>
 * Throws {@link ValidationException} with a descriptive message for every
 * rule violation. No Spring or JPA dependencies — fully unit-testable without
 * a Spring context.
 * </p>
 *
 * <p>Validation rules (in order):</p>
 * <ol>
 *   <li>name — non-null, non-blank</li>
 *   <li>email — non-null, non-blank, RFC 5322 format</li>
 *   <li>password — non-null, non-blank, ≥ 8 characters</li>
 *   <li>phoneNumber — non-null, non-blank</li>
 * </ol>
 */
public class UserValidator {

    /**
     * RFC 5322-compatible email pattern.
     * Covers the vast majority of real-world addresses while rejecting
     * obviously malformed strings (no local part, no domain, spaces, etc.).
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+"
            + "(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*"
            + "@"
            + "(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)"
            + "+[a-zA-Z]{2,}$"
    );

    private static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * Validates the given {@link CreateUserCommand}.
     *
     * @param command the command to validate; must not be {@code null}
     * @throws ValidationException if any field fails validation
     */
    public void validate(CreateUserCommand command) {
        validateName(command.name());
        validateEmail(command.email());
        validatePassword(command.password());
        validatePhoneNumber(command.phoneNumber());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void validateName(String name) {
        if (isBlankOrNull(name)) {
            throw new ValidationException("name is required");
        }
    }

    private void validateEmail(String email) {
        if (isBlankOrNull(email)) {
            throw new ValidationException("email is required");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("email format is invalid");
        }
    }

    private void validatePassword(String password) {
        if (isBlankOrNull(password)) {
            throw new ValidationException("password is required");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("password must be at least 8 characters");
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (isBlankOrNull(phoneNumber)) {
            throw new ValidationException("phoneNumber is required");
        }
    }

    private boolean isBlankOrNull(String value) {
        return value == null || value.isBlank();
    }
}
