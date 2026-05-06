package com.example.fleet.application;

import com.example.fleet.application.command.CreateDriverCommand;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.validator.DriverValidator;
import com.example.fleet.domain.model.DriverStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DriverValidator}.
 * No Spring context — pure JUnit 5.
 *
 * <p>Validation order: userId → cnh (null/blank) → cnh (alphanumeric) → cnh (length) → status</p>
 *
 * <p>Requirements covered: 2.1, 2.2, 2.3, 2.4, 4.1, 5.1</p>
 */
class DriverValidatorTest {

    private DriverValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DriverValidator();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private CreateDriverCommand validCommand() {
        return new CreateDriverCommand(
                UUID.randomUUID(),
                "ABC12345",
                DriverStatus.ACTIVE
        );
    }

    private CreateDriverCommand withUserId(UUID userId) {
        return new CreateDriverCommand(userId, "ABC12345", DriverStatus.ACTIVE);
    }

    private CreateDriverCommand withCnh(String cnh) {
        return new CreateDriverCommand(UUID.randomUUID(), cnh, DriverStatus.ACTIVE);
    }

    private CreateDriverCommand withStatus(DriverStatus status) {
        return new CreateDriverCommand(UUID.randomUUID(), "ABC12345", status);
    }

    // -----------------------------------------------------------------------
    // Required-field: userId
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("userId field validation")
    class UserIdValidation {

        @Test
        @DisplayName("null userId throws ValidationException with descriptive message")
        void nullUserId_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withUserId(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("userId is required");
        }
    }

    // -----------------------------------------------------------------------
    // Required-field: cnh — null / blank
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("cnh field validation — null and blank")
    class CnhNullBlankValidation {

        @Test
        @DisplayName("null cnh throws ValidationException with descriptive message")
        void nullCnh_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withCnh(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cnh is required");
        }

        @ParameterizedTest(name = "blank cnh [{0}] throws ValidationException")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        @DisplayName("blank cnh throws ValidationException")
        void blankCnh_throwsValidationException(String cnh) {
            assertThatThrownBy(() -> validator.validate(withCnh(cnh)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cnh is required");
        }
    }

    // -----------------------------------------------------------------------
    // cnh — alphanumeric constraint
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("cnh field validation — alphanumeric constraint")
    class CnhAlphanumericValidation {

        @ParameterizedTest(name = "non-alphanumeric cnh [{0}] throws ValidationException")
        @ValueSource(strings = {
                "ABC-123",
                "12345!",
                "CNH 001",
                "abc@def",
                "123.456",
                "AB/CD",
                "12345#",
                "cnh_001"
        })
        @DisplayName("cnh with non-alphanumeric characters throws ValidationException")
        void nonAlphanumericCnh_throwsValidationException(String cnh) {
            assertThatThrownBy(() -> validator.validate(withCnh(cnh)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cnh must be alphanumeric");
        }

        @ParameterizedTest(name = "alphanumeric cnh [{0}] passes alphanumeric check")
        @ValueSource(strings = {"ABC123", "12345678901", "ABCDEFGHIJ", "a1b2c3"})
        @DisplayName("alphanumeric cnh values pass the alphanumeric check")
        void alphanumericCnh_doesNotThrowAlphanumericError(String cnh) {
            // These are valid alphanumeric values within length — should not throw alphanumeric error
            assertThatNoException().isThrownBy(() -> validator.validate(withCnh(cnh)));
        }
    }

    // -----------------------------------------------------------------------
    // cnh — length constraint
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("cnh field validation — length constraint")
    class CnhLengthValidation {

        @Test
        @DisplayName("cnh of exactly 21 characters throws ValidationException")
        void cnhOf21Chars_throwsValidationException() {
            String cnh = "A".repeat(21);
            assertThatThrownBy(() -> validator.validate(withCnh(cnh)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cnh must be between 1 and 20 characters");
        }

        @ParameterizedTest(name = "cnh of length > 20 [{0}] throws ValidationException")
        @ValueSource(strings = {
                "ABCDEFGHIJKLMNOPQRSTU",          // 21 chars
                "ABCDEFGHIJKLMNOPQRSTUV",         // 22 chars
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345" // 31 chars
        })
        @DisplayName("cnh longer than 20 characters throws ValidationException")
        void cnhLongerThan20Chars_throwsValidationException(String cnh) {
            assertThatThrownBy(() -> validator.validate(withCnh(cnh)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cnh must be between 1 and 20 characters");
        }

        @Test
        @DisplayName("cnh of exactly 1 character passes length check")
        void cnhOf1Char_doesNotThrow() {
            assertThatNoException().isThrownBy(() -> validator.validate(withCnh("A")));
        }

        @Test
        @DisplayName("cnh of exactly 20 characters passes length check")
        void cnhOf20Chars_doesNotThrow() {
            String cnh = "A".repeat(20);
            assertThatNoException().isThrownBy(() -> validator.validate(withCnh(cnh)));
        }
    }

    // -----------------------------------------------------------------------
    // Required-field: status
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("status field validation")
    class StatusValidation {

        @Test
        @DisplayName("null status throws ValidationException with descriptive message")
        void nullStatus_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withStatus(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("status is required");
        }

        @Test
        @DisplayName("ACTIVE status passes validation")
        void activeStatus_doesNotThrow() {
            assertThatNoException().isThrownBy(() -> validator.validate(withStatus(DriverStatus.ACTIVE)));
        }

        @Test
        @DisplayName("INACTIVE status passes validation")
        void inactiveStatus_doesNotThrow() {
            assertThatNoException().isThrownBy(() -> validator.validate(withStatus(DriverStatus.INACTIVE)));
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

        @ParameterizedTest(name = "valid cnh [{0}] does not throw")
        @ValueSource(strings = {
                "A",
                "12345678901",
                "ABCDEFGHIJ",
                "a1b2c3d4e5",
                "ABCDEFGHIJKLMNOPQRST"  // exactly 20 chars
        })
        @DisplayName("various valid CNH values are accepted")
        void variousValidCnhValues_doNotThrow(String cnh) {
            assertThatNoException().isThrownBy(() -> validator.validate(withCnh(cnh)));
        }
    }
}
