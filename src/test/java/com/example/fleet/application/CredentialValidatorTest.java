package com.example.fleet.application;

import com.example.fleet.application.command.LoginCommand;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.validator.CredentialValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CredentialValidator}.
 * No Spring context — pure JUnit 5.
 */
class CredentialValidatorTest {

    private CredentialValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CredentialValidator();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private LoginCommand validCommand() {
        return new LoginCommand("jane.doe@example.com", "S3cur3P@ss");
    }

    private LoginCommand withEmail(String email) {
        return new LoginCommand(email, "S3cur3P@ss");
    }

    private LoginCommand withPassword(String password) {
        return new LoginCommand("jane.doe@example.com", password);
    }

    // -----------------------------------------------------------------------
    // email field validation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("email field validation")
    class EmailValidation {

        @Test
        @DisplayName("null email throws ValidationException with 'email is required'")
        void nullEmail_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withEmail(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("email is required");
        }

        @ParameterizedTest(name = "blank email [{0}] throws ValidationException")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        @DisplayName("blank email throws ValidationException with 'email is required'")
        void blankEmail_throwsValidationException(String email) {
            assertThatThrownBy(() -> validator.validate(withEmail(email)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("email is required");
        }
    }

    // -----------------------------------------------------------------------
    // password field validation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("password field validation")
    class PasswordValidation {

        @Test
        @DisplayName("null password throws ValidationException with 'password is required'")
        void nullPassword_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withPassword(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("password is required");
        }

        @ParameterizedTest(name = "blank password [{0}] throws ValidationException")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        @DisplayName("blank password throws ValidationException with 'password is required'")
        void blankPassword_throwsValidationException(String password) {
            assertThatThrownBy(() -> validator.validate(withPassword(password)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("password is required");
        }
    }

    // -----------------------------------------------------------------------
    // Valid input
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("valid input")
    class ValidInput {

        @Test
        @DisplayName("non-blank email and password does not throw any exception")
        void validCommand_doesNotThrow() {
            assertThatNoException().isThrownBy(() -> validator.validate(validCommand()));
        }

        @ParameterizedTest(name = "email [{0}] with valid password does not throw")
        @ValueSource(strings = {
                "user@example.com",
                "admin@fleet.io",
                "jane.doe+tag@sub.domain.org"
        })
        @DisplayName("various non-blank emails with valid password do not throw")
        void variousValidEmails_doNotThrow(String email) {
            assertThatNoException().isThrownBy(() -> validator.validate(withEmail(email)));
        }

        @ParameterizedTest(name = "password [{0}] with valid email does not throw")
        @ValueSource(strings = {"pass", "S3cur3P@ss", "a", "hunter2"})
        @DisplayName("any non-blank password with valid email does not throw")
        void variousNonBlankPasswords_doNotThrow(String password) {
            assertThatNoException().isThrownBy(() -> validator.validate(withPassword(password)));
        }
    }
}
