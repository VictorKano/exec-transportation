package com.example.fleet.application.validator;

import com.example.fleet.application.command.LoginCommand;
import com.example.fleet.application.exception.ValidationException;

/**
 * Application-layer validator for {@link LoginCommand}.
 * <p>
 * Throws {@link ValidationException} with a descriptive message for every
 * rule violation. No Spring or JPA dependencies — fully unit-testable without
 * a Spring context.
 * </p>
 *
 * <p>Validation rules (in order):</p>
 * <ol>
 *   <li>email — non-null, non-blank</li>
 *   <li>password — non-null, non-blank</li>
 * </ol>
 */
public class CredentialValidator {

    /**
     * Validates the given {@link LoginCommand}.
     *
     * @param command the command to validate; must not be {@code null}
     * @throws ValidationException if the email or password is null or blank
     */
    public void validate(LoginCommand command) {
        if (isBlankOrNull(command.email())) {
            throw new ValidationException("email is required");
        }
        if (isBlankOrNull(command.password())) {
            throw new ValidationException("password is required");
        }
    }

    private boolean isBlankOrNull(String value) {
        return value == null || value.isBlank();
    }
}
