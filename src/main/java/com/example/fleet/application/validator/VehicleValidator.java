package com.example.fleet.application.validator;

import com.example.fleet.application.command.CreateVehicleCommand;
import com.example.fleet.application.exception.ValidationException;

import java.time.Year;

/**
 * Application-layer validator for {@link CreateVehicleCommand}.
 *
 * <p>Throws {@link ValidationException} with a descriptive message for every
 * rule violation. No Spring or JPA dependencies — fully unit-testable without
 * a Spring context.</p>
 *
 * <p>Validation rules (in order):</p>
 * <ol>
 *   <li>plate — non-null, non-blank → {@code "plate is required"}</li>
 *   <li>brand — non-null, non-blank → {@code "brand is required"}</li>
 *   <li>model — non-null, non-blank → {@code "model is required"}</li>
 *   <li>year — non-null → {@code "year is required"}</li>
 *   <li>year — &ge; 1886 → {@code "year must be 1886 or later"}</li>
 *   <li>year — &le; currentYear + 1 → {@code "year must not exceed current year + 1"}</li>
 * </ol>
 */
public class VehicleValidator {

    private static final int FIRST_AUTOMOBILE_YEAR = 1886;

    /**
     * Validates the given {@link CreateVehicleCommand}.
     *
     * @param command the command to validate; must not be {@code null}
     * @throws ValidationException if any field fails validation
     */
    public void validate(CreateVehicleCommand command) {
        validatePlate(command.plate());
        validateBrand(command.brand());
        validateModel(command.model());
        validateYear(command.year());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void validatePlate(String plate) {
        if (plate == null || plate.isBlank()) {
            throw new ValidationException("plate is required");
        }
    }

    private void validateBrand(String brand) {
        if (brand == null || brand.isBlank()) {
            throw new ValidationException("brand is required");
        }
    }

    private void validateModel(String model) {
        if (model == null || model.isBlank()) {
            throw new ValidationException("model is required");
        }
    }

    private void validateYear(Integer year) {
        if (year == null) {
            throw new ValidationException("year is required");
        }
        if (year < FIRST_AUTOMOBILE_YEAR) {
            throw new ValidationException("year must be 1886 or later");
        }
        int maxYear = Year.now().getValue() + 1;
        if (year > maxYear) {
            throw new ValidationException("year must not exceed current year + 1");
        }
    }
}
