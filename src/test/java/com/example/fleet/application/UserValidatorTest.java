package com.example.fleet.application;

import com.example.fleet.application.command.CreateUserCommand;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.validator.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link UserValidator}.
 * No Spring context — pure JUnit 5.
 */
class UserValidatorTest {

    private UserValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UserValidator();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private CreateUserCommand validCommand() {
        return new CreateUserCommand(
                "Jane Doe",
                "jane.doe@example.com",
                "S3cur3P@ss",
                "+1-555-0100"
        );
    }

    private CreateUserCommand withName(String name) {
        return new CreateUserCommand(name, "jane.doe@example.com", "S3cur3P@ss", "+1-555-0100");
    }

    private CreateUserCommand withEmail(String email) {
        return new CreateUserCommand("Jane Doe", email, "S3cur3P@ss", "+1-555-0100");
    }

    private CreateUserCommand withPassword(String password) {
        return new CreateUserCommand("Jane Doe", "jane.doe@example.com", password, "+1-555-0100");
    }

    private CreateUserCommand withPhoneNumber(String phoneNumber) {
        return new CreateUserCommand("Jane Doe", "jane.doe@example.com", "S3cur3P@ss", phoneNumber);
    }

    // -----------------------------------------------------------------------
    // Required-field: name
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("name field validation")
    class NameValidation {

        @Test
        @DisplayName("null name throws ValidationException with descriptive message")
        void nullName_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withName(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("name is required");
        }

        @ParameterizedTest(name = "blank name [{0}] throws ValidationException")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        @DisplayName("blank name throws ValidationException")
        void blankName_throwsValidationException(String name) {
            assertThatThrownBy(() -> validator.validate(withName(name)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("name is required");
        }
    }

    // -----------------------------------------------------------------------
    // Required-field: email
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("email field validation")
    class EmailValidation {

        @Test
        @DisplayName("null email throws ValidationException with descriptive message")
        void nullEmail_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withEmail(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("email is required");
        }

        @ParameterizedTest(name = "blank email [{0}] throws ValidationException")
        @ValueSource(strings = {"", " ", "   ", "\t"})
        @DisplayName("blank email throws ValidationException")
        void blankEmail_throwsValidationException(String email) {
            assertThatThrownBy(() -> validator.validate(withEmail(email)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("email is required");
        }

        @ParameterizedTest(name = "invalid email [{0}] throws ValidationException")
        @ValueSource(strings = {
                "notanemail",
                "missing@",
                "@nodomain.com",
                "spaces in@email.com",
                "double@@domain.com",
                "no-at-sign",
                "trailing-dot@domain.",
                "plain-text"
        })
        @DisplayName("invalid email format throws ValidationException")
        void invalidEmailFormat_throwsValidationException(String email) {
            assertThatThrownBy(() -> validator.validate(withEmail(email)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("email format is invalid");
        }

        @ParameterizedTest(name = "valid email [{0}] passes validation")
        @ValueSource(strings = {
                "user@example.com",
                "user.name+tag@sub.domain.org",
                "user123@domain.co.uk"
        })
        @DisplayName("valid email formats pass validation")
        void validEmailFormat_doesNotThrow(String email) {
            assertThatNoException().isThrownBy(() -> validator.validate(withEmail(email)));
        }
    }

    // -----------------------------------------------------------------------
    // Required-field: password
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("password field validation")
    class PasswordValidation {

        @Test
        @DisplayName("null password throws ValidationException with descriptive message")
        void nullPassword_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withPassword(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("password is required");
        }

        @ParameterizedTest(name = "blank password [{0}] throws ValidationException")
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("blank password throws ValidationException")
        void blankPassword_throwsValidationException(String password) {
            assertThatThrownBy(() -> validator.validate(withPassword(password)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("password is required");
        }

        @ParameterizedTest(name = "password of length {0} is too short")
        @ValueSource(strings = {"a", "ab", "abc", "abcd", "abcde", "abcdef", "abcdefg"})
        @DisplayName("password shorter than 8 characters throws ValidationException")
        void shortPassword_throwsValidationException(String password) {
            assertThatThrownBy(() -> validator.validate(withPassword(password)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("password must be at least 8 characters");
        }

        @Test
        @DisplayName("password of exactly 8 characters passes validation")
        void passwordExactly8Chars_doesNotThrow() {
            assertThatNoException().isThrownBy(() -> validator.validate(withPassword("abcdefgh")));
        }

        @Test
        @DisplayName("password longer than 8 characters passes validation")
        void passwordLongerThan8Chars_doesNotThrow() {
            assertThatNoException().isThrownBy(() -> validator.validate(withPassword("S3cur3P@ssword")));
        }
    }

    // -----------------------------------------------------------------------
    // Required-field: phoneNumber
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("phoneNumber field validation")
    class PhoneNumberValidation {

        @Test
        @DisplayName("null phoneNumber throws ValidationException with descriptive message")
        void nullPhoneNumber_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withPhoneNumber(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("phoneNumber is required");
        }

        @ParameterizedTest(name = "blank phoneNumber [{0}] throws ValidationException")
        @ValueSource(strings = {"", " ", "   ", "\t"})
        @DisplayName("blank phoneNumber throws ValidationException")
        void blankPhoneNumber_throwsValidationException(String phoneNumber) {
            assertThatThrownBy(() -> validator.validate(withPhoneNumber(phoneNumber)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("phoneNumber is required");
        }
    }

    // -----------------------------------------------------------------------
    // Valid input
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("valid input")
    class ValidInput {

        @Test
        @DisplayName("fully valid command does not throw any exception")
        void validCommand_doesNotThrow() {
            assertThatNoException().isThrownBy(() -> validator.validate(validCommand()));
        }

        @ParameterizedTest(name = "valid command with email [{0}] does not throw")
        @ValueSource(strings = {
                "simple@example.com",
                "very.common@example.com",
                "disposable.style.email.with+symbol@example.com",
                "other.email-with-hyphen@example.com",
                "user%example.com@example.org"
        })
        @DisplayName("various valid email formats are accepted")
        void variousValidEmails_doNotThrow(String email) {
            assertThatNoException().isThrownBy(() -> validator.validate(withEmail(email)));
        }
    }
}
