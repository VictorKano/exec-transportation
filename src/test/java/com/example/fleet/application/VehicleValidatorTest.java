package com.example.fleet.application;

import com.example.fleet.application.command.CreateVehicleCommand;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.validator.VehicleValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Year;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VehicleValidator}.
 * No Spring context — pure JUnit 5.
 *
 * <p>Validation order: plate → brand → model → year (null check) → year (range check)</p>
 *
 * <p>Requirements covered: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.1, 3.2, 3.3</p>
 */
class VehicleValidatorTest {

    private VehicleValidator validator;

    @BeforeEach
    void setUp() {
        validator = new VehicleValidator();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private CreateVehicleCommand validCommand() {
        return new CreateVehicleCommand("ABC1234", "Toyota", "Corolla", 2020, null);
    }

    private CreateVehicleCommand withPlate(String plate) {
        return new CreateVehicleCommand(plate, "Toyota", "Corolla", 2020, null);
    }

    private CreateVehicleCommand withBrand(String brand) {
        return new CreateVehicleCommand("ABC1234", brand, "Corolla", 2020, null);
    }

    private CreateVehicleCommand withModel(String model) {
        return new CreateVehicleCommand("ABC1234", "Toyota", model, 2020, null);
    }

    private CreateVehicleCommand withYear(Integer year) {
        return new CreateVehicleCommand("ABC1234", "Toyota", "Corolla", year, null);
    }

    // -----------------------------------------------------------------------
    // plate field validation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("plate field validation")
    class PlateValidation {

        @Test
        @DisplayName("null plate throws ValidationException with 'plate is required'")
        void nullPlate_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withPlate(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("plate is required");
        }

        @ParameterizedTest(name = "blank plate [{0}] throws ValidationException with 'plate is required'")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        @DisplayName("blank plate throws ValidationException with 'plate is required'")
        void blankPlate_throwsValidationException(String plate) {
            assertThatThrownBy(() -> validator.validate(withPlate(plate)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("plate is required");
        }
    }

    // -----------------------------------------------------------------------
    // brand field validation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("brand field validation")
    class BrandValidation {

        @Test
        @DisplayName("null brand throws ValidationException with 'brand is required'")
        void nullBrand_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withBrand(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("brand is required");
        }

        @ParameterizedTest(name = "blank brand [{0}] throws ValidationException with 'brand is required'")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        @DisplayName("blank brand throws ValidationException with 'brand is required'")
        void blankBrand_throwsValidationException(String brand) {
            assertThatThrownBy(() -> validator.validate(withBrand(brand)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("brand is required");
        }
    }

    // -----------------------------------------------------------------------
    // model field validation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("model field validation")
    class ModelValidation {

        @Test
        @DisplayName("null model throws ValidationException with 'model is required'")
        void nullModel_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withModel(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("model is required");
        }

        @ParameterizedTest(name = "blank model [{0}] throws ValidationException with 'model is required'")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        @DisplayName("blank model throws ValidationException with 'model is required'")
        void blankModel_throwsValidationException(String model) {
            assertThatThrownBy(() -> validator.validate(withModel(model)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("model is required");
        }
    }

    // -----------------------------------------------------------------------
    // year field validation — null check
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("year field validation — null check")
    class YearNullValidation {

        @Test
        @DisplayName("null year throws ValidationException with 'year is required'")
        void nullYear_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withYear(null)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("year is required");
        }
    }

    // -----------------------------------------------------------------------
    // year field validation — range check
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("year field validation — range check")
    class YearRangeValidation {

        @Test
        @DisplayName("year = 1885 throws ValidationException with 'year must be 1886 or later'")
        void year1885_throwsValidationException() {
            assertThatThrownBy(() -> validator.validate(withYear(1885)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("year must be 1886 or later");
        }

        @Test
        @DisplayName("year = 1886 (first automobile year) does not throw")
        void year1886_doesNotThrow() {
            assertThatNoException().isThrownBy(() -> validator.validate(withYear(1886)));
        }

        @Test
        @DisplayName("year = currentYear + 1 does not throw")
        void yearCurrentPlusOne_doesNotThrow() {
            int maxYear = Year.now().getValue() + 1;
            assertThatNoException().isThrownBy(() -> validator.validate(withYear(maxYear)));
        }

        @Test
        @DisplayName("year = currentYear + 2 throws ValidationException with 'year must not exceed current year + 1'")
        void yearCurrentPlusTwo_throwsValidationException() {
            int tooFarFuture = Year.now().getValue() + 2;
            assertThatThrownBy(() -> validator.validate(withYear(tooFarFuture)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("year must not exceed current year + 1");
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

        @Test
        @DisplayName("valid command with driverId does not throw any exception")
        void validCommandWithDriverId_doesNotThrow() {
            CreateVehicleCommand command = new CreateVehicleCommand(
                    "XYZ9876", "Ford", "F-150", 2022, UUID.randomUUID());
            assertThatNoException().isThrownBy(() -> validator.validate(command));
        }
    }
}
