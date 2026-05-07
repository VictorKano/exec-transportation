package com.example.fleet.application;

import com.example.fleet.application.command.CreateVehicleCommand;
import com.example.fleet.application.exception.DriverNotFoundException;
import com.example.fleet.application.exception.DuplicatePlateException;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.response.VehicleResponse;
import com.example.fleet.application.service.VehicleService;
import com.example.fleet.application.validator.VehicleValidator;
import com.example.fleet.domain.model.Driver;
import com.example.fleet.domain.model.DriverStatus;
import com.example.fleet.domain.model.Vehicle;
import com.example.fleet.domain.repository.DriverRepository;
import com.example.fleet.domain.repository.VehicleRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

import java.time.Year;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Property-based tests for {@link VehicleService} and {@link VehicleValidator}.
 *
 * <p>Uses jqwik to verify universal correctness properties across a wide input space.
 * No Spring context — pure jqwik + JUnit 5 with in-memory stubs.</p>
 *
 * <p>Requirements covered: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 2.2, 2.4, 2.6, 2.8,
 * 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4</p>
 */
class VehicleServicePropertyTest {

    // -----------------------------------------------------------------------
    // Generators
    // -----------------------------------------------------------------------

    /** Arbitrary that produces a valid non-blank plate (alphanumeric, 1–20 chars). */
    @Provide
    Arbitrary<String> validPlate() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    /** Arbitrary that produces a valid non-blank brand string. */
    @Provide
    Arbitrary<String> validBrand() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    /** Arbitrary that produces a valid non-blank model string. */
    @Provide
    Arbitrary<String> validModel() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    /** Arbitrary that produces a valid year in [1886, currentYear + 1]. */
    @Provide
    Arbitrary<Integer> validYear() {
        int maxYear = Year.now().getValue() + 1;
        return Arbitraries.integers().between(1886, maxYear);
    }

    /** Arbitrary that produces a year strictly below 1886. */
    @Provide
    Arbitrary<Integer> yearBelowMin() {
        return Arbitraries.integers().between(Integer.MIN_VALUE, 1885);
    }

    /** Arbitrary that produces a year strictly above currentYear + 1. */
    @Provide
    Arbitrary<Integer> yearAboveMax() {
        int tooFarFuture = Year.now().getValue() + 2;
        return Arbitraries.integers().between(tooFarFuture, Integer.MAX_VALUE);
    }

    /** Arbitrary that produces a whitespace-only string (blank but non-null). */
    @Provide
    Arbitrary<String> blankString() {
        return Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r', '\f')
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    /** Arbitrary that produces a valid UUID to use as a driverId. */
    @Provide
    Arbitrary<UUID> validDriverId() {
        return Arbitraries.create(UUID::randomUUID);
    }

    // -----------------------------------------------------------------------
    // Stub factories
    // -----------------------------------------------------------------------

    /**
     * Builds a {@link VehicleRepository} stub where {@code existsByPlate} always returns
     * {@code false} and {@code save} returns the vehicle as-is.
     */
    private VehicleRepository freshVehicleRepository() {
        return new VehicleRepository() {
            @Override
            public Vehicle save(Vehicle vehicle) { return vehicle; }

            @Override
            public boolean existsByPlate(String plate) { return false; }
        };
    }

    /**
     * Builds a {@link VehicleRepository} stub where {@code existsByPlate} always returns
     * {@code true} (simulating a pre-existing duplicate plate). Tracks whether {@code save}
     * was called via the provided boolean array.
     */
    private VehicleRepository duplicatePlateRepository(boolean[] saveCalled) {
        return new VehicleRepository() {
            @Override
            public Vehicle save(Vehicle vehicle) {
                saveCalled[0] = true;
                return vehicle;
            }

            @Override
            public boolean existsByPlate(String plate) { return true; }
        };
    }

    /**
     * Builds a {@link VehicleRepository} stub that tracks whether {@code existsByPlate}
     * and {@code save} were called.
     */
    private VehicleRepository trackingVehicleRepository(boolean[] existsByPlateCalled, boolean[] saveCalled) {
        return new VehicleRepository() {
            @Override
            public Vehicle save(Vehicle vehicle) {
                saveCalled[0] = true;
                return vehicle;
            }

            @Override
            public boolean existsByPlate(String plate) {
                existsByPlateCalled[0] = true;
                return false;
            }
        };
    }

    /**
     * Builds a {@link DriverRepository} stub where {@code findById} always returns a
     * synthetic {@link Driver} for any id.
     */
    private DriverRepository knownDriverRepository() {
        return new DriverRepository() {
            @Override
            public Driver save(Driver driver) { return driver; }

            @Override
            public boolean existsByCnh(String cnh) { return false; }

            @Override
            public Optional<Driver> findById(UUID id) {
                return Optional.of(new Driver(id, UUID.randomUUID(), "CNH12345", DriverStatus.ACTIVE));
            }
        };
    }

    /**
     * Builds a {@link DriverRepository} stub where {@code findById} always returns empty
     * (simulating an unknown driver). Tracks whether {@code save} was called.
     */
    private DriverRepository unknownDriverRepository(boolean[] saveCalled) {
        return new DriverRepository() {
            @Override
            public Driver save(Driver driver) {
                saveCalled[0] = true;
                return driver;
            }

            @Override
            public boolean existsByCnh(String cnh) { return false; }

            @Override
            public Optional<Driver> findById(UUID id) { return Optional.empty(); }
        };
    }

    /**
     * Builds a {@link DriverRepository} stub that is never expected to be called
     * (null driverId path).
     */
    private DriverRepository noCallDriverRepository() {
        return new DriverRepository() {
            @Override
            public Driver save(Driver driver) { return driver; }

            @Override
            public boolean existsByCnh(String cnh) { return false; }

            @Override
            public Optional<Driver> findById(UUID id) { return Optional.empty(); }
        };
    }

    // -----------------------------------------------------------------------
    // Property 1: Valid command produces a correct VehicleResponse
    // -----------------------------------------------------------------------

    /**
     * Property 1a: Valid command with null driverId produces a correct VehicleResponse.
     *
     * <p>For any valid {@link CreateVehicleCommand} (non-blank plate/brand/model,
     * year in [1886, currentYear+1], null driverId) where the plate is not a duplicate,
     * {@link VehicleService#createVehicle} SHALL return a {@link VehicleResponse} where all
     * fields match the command, {@code id} is non-null, and {@code driverId} is null.</p>
     *
     * <p>Validates: Requirements 1.1, 1.2, 1.4, 1.5, 1.6, 1.7, 4.3, 5.1</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-vehicle")
    @Tag("Property_1_Valid_command_produces_correct_VehicleResponse")
    void validCommand_withNullDriverId_alwaysProducesCorrectVehicleResponse(
            @ForAll("validPlate") String plate,
            @ForAll("validBrand") String brand,
            @ForAll("validModel") String model,
            @ForAll("validYear") Integer year) {

        VehicleRepository vehicleRepository = freshVehicleRepository();
        DriverRepository driverRepository = noCallDriverRepository();
        VehicleService vehicleService = new VehicleService(vehicleRepository, driverRepository, new VehicleValidator());

        CreateVehicleCommand command = new CreateVehicleCommand(plate, brand, model, year, null);

        VehicleResponse response = vehicleService.createVehicle(command);

        assertThat(response.id())
                .as("VehicleResponse.id must be non-null")
                .isNotNull();

        assertThat(response.plate())
                .as("VehicleResponse.plate must equal command.plate()")
                .isEqualTo(plate);

        assertThat(response.brand())
                .as("VehicleResponse.brand must equal command.brand()")
                .isEqualTo(brand);

        assertThat(response.model())
                .as("VehicleResponse.model must equal command.model()")
                .isEqualTo(model);

        assertThat(response.year())
                .as("VehicleResponse.year must equal command.year()")
                .isEqualTo(year);

        assertThat(response.driverId())
                .as("VehicleResponse.driverId must be null when command.driverId() is null")
                .isNull();
    }

    /**
     * Property 1b: Valid command with an existing driverId produces a correct VehicleResponse.
     *
     * <p>For any valid {@link CreateVehicleCommand} (non-blank plate/brand/model,
     * year in [1886, currentYear+1], non-null driverId that resolves to an existing driver)
     * where the plate is not a duplicate, {@link VehicleService#createVehicle} SHALL return a
     * {@link VehicleResponse} where all fields match the command, {@code id} is non-null, and
     * {@code driverId} equals the command's {@code driverId}.</p>
     *
     * <p>Validates: Requirements 1.1, 1.2, 1.4, 1.5, 1.6, 1.7, 4.3, 5.2</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-vehicle")
    @Tag("Property_1_Valid_command_produces_correct_VehicleResponse")
    void validCommand_withExistingDriverId_alwaysProducesCorrectVehicleResponse(
            @ForAll("validPlate") String plate,
            @ForAll("validBrand") String brand,
            @ForAll("validModel") String model,
            @ForAll("validYear") Integer year,
            @ForAll("validDriverId") UUID driverId) {

        VehicleRepository vehicleRepository = freshVehicleRepository();
        DriverRepository driverRepository = knownDriverRepository();
        VehicleService vehicleService = new VehicleService(vehicleRepository, driverRepository, new VehicleValidator());

        CreateVehicleCommand command = new CreateVehicleCommand(plate, brand, model, year, driverId);

        VehicleResponse response = vehicleService.createVehicle(command);

        assertThat(response.id())
                .as("VehicleResponse.id must be non-null")
                .isNotNull();

        assertThat(response.plate())
                .as("VehicleResponse.plate must equal command.plate()")
                .isEqualTo(plate);

        assertThat(response.brand())
                .as("VehicleResponse.brand must equal command.brand()")
                .isEqualTo(brand);

        assertThat(response.model())
                .as("VehicleResponse.model must equal command.model()")
                .isEqualTo(model);

        assertThat(response.year())
                .as("VehicleResponse.year must equal command.year()")
                .isEqualTo(year);

        assertThat(response.driverId())
                .as("VehicleResponse.driverId must equal command.driverId() when driver exists")
                .isEqualTo(driverId);
    }

    // -----------------------------------------------------------------------
    // Property 2: Two valid commands with different plates produce different UUIDs
    // -----------------------------------------------------------------------

    /**
     * Property 2: Two valid commands with different plates produce different UUIDs.
     *
     * <p>For any two valid {@link CreateVehicleCommand} objects with different {@code plate}
     * values, the {@link VehicleResponse} objects returned by
     * {@link VehicleService#createVehicle} SHALL have different {@code id} values.</p>
     *
     * <p>Validates: Requirements 1.3</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-vehicle")
    @Tag("Property_2_Different_plates_produce_different_UUIDs")
    void twoCommandsWithDifferentPlates_alwaysProduceDifferentIds(
            @ForAll("validPlate") String plate1,
            @ForAll("validBrand") String brand1,
            @ForAll("validModel") String model1,
            @ForAll("validYear") Integer year1,
            @ForAll("validPlate") String plate2,
            @ForAll("validBrand") String brand2,
            @ForAll("validModel") String model2,
            @ForAll("validYear") Integer year2) {

        // Ensure the two plates are different so both commands can succeed
        Assume.that(!plate1.equals(plate2));

        VehicleRepository vehicleRepository = freshVehicleRepository();
        DriverRepository driverRepository = noCallDriverRepository();
        VehicleService vehicleService = new VehicleService(vehicleRepository, driverRepository, new VehicleValidator());

        CreateVehicleCommand command1 = new CreateVehicleCommand(plate1, brand1, model1, year1, null);
        CreateVehicleCommand command2 = new CreateVehicleCommand(plate2, brand2, model2, year2, null);

        VehicleResponse response1 = vehicleService.createVehicle(command1);
        VehicleResponse response2 = vehicleService.createVehicle(command2);

        assertThat(response1.id())
                .as("Two vehicles created with different plates must receive different UUIDs")
                .isNotEqualTo(response2.id());
    }

    // -----------------------------------------------------------------------
    // Property 3: Blank required fields are always rejected
    // -----------------------------------------------------------------------

    /**
     * Property 3: Blank required fields are always rejected.
     *
     * <p>For any {@link CreateVehicleCommand} where {@code plate}, {@code brand}, or
     * {@code model} is a string composed entirely of whitespace characters,
     * {@link VehicleValidator#validate} SHALL throw a {@link ValidationException} with the
     * appropriate {@code "<field> is required"} message, and {@link VehicleService} SHALL NOT
     * call {@code VehicleRepository.existsByPlate} or {@code VehicleRepository.save}.</p>
     *
     * <p>Validates: Requirements 2.2, 2.4, 2.6, 2.8</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-vehicle")
    @Tag("Property_3_Blank_required_fields_are_always_rejected")
    void blankRequiredField_alwaysThrowsValidationException(
            @ForAll("blankFieldCommand") CreateVehicleCommand commandWithBlankField) {

        boolean[] existsByPlateCalled = {false};
        boolean[] saveCalled = {false};

        VehicleRepository vehicleRepository = trackingVehicleRepository(existsByPlateCalled, saveCalled);
        DriverRepository driverRepository = noCallDriverRepository();
        VehicleService vehicleService = new VehicleService(vehicleRepository, driverRepository, new VehicleValidator());

        Throwable thrown = catchThrowable(() -> vehicleService.createVehicle(commandWithBlankField));

        assertThat(thrown)
                .as("ValidationException must be thrown for a blank required field")
                .isInstanceOf(ValidationException.class);

        assertThat(existsByPlateCalled[0])
                .as("VehicleRepository.existsByPlate must NOT be called when validation fails")
                .isFalse();

        assertThat(saveCalled[0])
                .as("VehicleRepository.save must NOT be called when validation fails")
                .isFalse();
    }

    /** Produces commands with exactly one blank required field (plate, brand, or model). */
    @Provide
    Arbitrary<CreateVehicleCommand> blankFieldCommand() {
        int validYear = Year.now().getValue();

        // blank plate
        Arbitrary<CreateVehicleCommand> blankPlate = Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(1)
                .ofMaxLength(10)
                .map(s -> new CreateVehicleCommand(s, "Toyota", "Corolla", validYear, null));

        // blank brand
        Arbitrary<CreateVehicleCommand> blankBrand = Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(1)
                .ofMaxLength(10)
                .map(s -> new CreateVehicleCommand("ABC1234", s, "Corolla", validYear, null));

        // blank model
        Arbitrary<CreateVehicleCommand> blankModel = Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(1)
                .ofMaxLength(10)
                .map(s -> new CreateVehicleCommand("ABC1234", "Toyota", s, validYear, null));

        return Arbitraries.oneOf(blankPlate, blankBrand, blankModel);
    }

    // -----------------------------------------------------------------------
    // Property 4: Duplicate plate always throws DuplicatePlateException without saving
    // -----------------------------------------------------------------------

    /**
     * Property 4: Duplicate plate always throws DuplicatePlateException without saving.
     *
     * <p>For any valid {@link CreateVehicleCommand} where
     * {@code VehicleRepository.existsByPlate} returns {@code true},
     * {@link VehicleService#createVehicle} SHALL throw a {@link DuplicatePlateException}
     * and SHALL NOT call {@code VehicleRepository.save}.</p>
     *
     * <p>Validates: Requirements 4.1, 4.2</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-vehicle")
    @Tag("Property_4_Duplicate_plate_throws_without_saving")
    void duplicatePlate_alwaysThrowsDuplicatePlateExceptionWithoutSaving(
            @ForAll("validPlate") String plate,
            @ForAll("validBrand") String brand,
            @ForAll("validModel") String model,
            @ForAll("validYear") Integer year) {

        boolean[] saveCalled = {false};

        VehicleRepository vehicleRepository = duplicatePlateRepository(saveCalled);
        DriverRepository driverRepository = noCallDriverRepository();
        VehicleService vehicleService = new VehicleService(vehicleRepository, driverRepository, new VehicleValidator());

        CreateVehicleCommand command = new CreateVehicleCommand(plate, brand, model, year, null);

        Throwable thrown = catchThrowable(() -> vehicleService.createVehicle(command));

        assertThat(thrown)
                .as("DuplicatePlateException must be thrown when plate already exists")
                .isInstanceOf(DuplicatePlateException.class);

        assertThat(saveCalled[0])
                .as("VehicleRepository.save must NOT be called when plate is a duplicate")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Property 5: Unknown driverId always throws DriverNotFoundException without saving
    // -----------------------------------------------------------------------

    /**
     * Property 5: Unknown driverId always throws DriverNotFoundException without saving.
     *
     * <p>For any valid {@link CreateVehicleCommand} with a non-null {@code driverId} where
     * {@code DriverRepository.findById} returns empty, {@link VehicleService#createVehicle}
     * SHALL throw a {@link DriverNotFoundException} and SHALL NOT call
     * {@code VehicleRepository.save}.</p>
     *
     * <p>Validates: Requirements 5.3, 5.4</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-vehicle")
    @Tag("Property_5_Unknown_driverId_throws_without_saving")
    void unknownDriverId_alwaysThrowsDriverNotFoundExceptionWithoutSaving(
            @ForAll("validPlate") String plate,
            @ForAll("validBrand") String brand,
            @ForAll("validModel") String model,
            @ForAll("validYear") Integer year,
            @ForAll("validDriverId") UUID driverId) {

        boolean[] saveCalled = {false};

        VehicleRepository vehicleRepository = freshVehicleRepository();
        DriverRepository driverRepository = unknownDriverRepository(saveCalled);
        VehicleService vehicleService = new VehicleService(vehicleRepository, driverRepository, new VehicleValidator());

        CreateVehicleCommand command = new CreateVehicleCommand(plate, brand, model, year, driverId);

        Throwable thrown = catchThrowable(() -> vehicleService.createVehicle(command));

        assertThat(thrown)
                .as("DriverNotFoundException must be thrown when driverId is not found")
                .isInstanceOf(DriverNotFoundException.class);

        assertThat(saveCalled[0])
                .as("VehicleRepository.save must NOT be called when driver is not found")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Property 6: Year below 1886 is always rejected
    // -----------------------------------------------------------------------

    /**
     * Property 6: Year below 1886 is always rejected.
     *
     * <p>For any integer year strictly less than 1886, {@link VehicleValidator#validate}
     * SHALL throw a {@link ValidationException} with the message
     * {@code "year must be 1886 or later"}.</p>
     *
     * <p>Validates: Requirements 3.1</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-vehicle")
    @Tag("Property_6_Year_below_1886_is_always_rejected")
    void yearBelowMin_alwaysThrowsValidationException(
            @ForAll("yearBelowMin") Integer year) {

        VehicleValidator validator = new VehicleValidator();
        CreateVehicleCommand command = new CreateVehicleCommand("ABC1234", "Toyota", "Corolla", year, null);

        Throwable thrown = catchThrowable(() -> validator.validate(command));

        assertThat(thrown)
                .as("ValidationException must be thrown for year < 1886")
                .isInstanceOf(ValidationException.class)
                .hasMessage("year must be 1886 or later");
    }

    // -----------------------------------------------------------------------
    // Property 7: Year above currentYear + 1 is always rejected
    // -----------------------------------------------------------------------

    /**
     * Property 7: Year above currentYear + 1 is always rejected.
     *
     * <p>For any integer year strictly greater than the current calendar year plus 1,
     * {@link VehicleValidator#validate} SHALL throw a {@link ValidationException} with the
     * message {@code "year must not exceed current year + 1"}.</p>
     *
     * <p>Validates: Requirements 3.2</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-vehicle")
    @Tag("Property_7_Year_above_currentYear_plus_1_is_always_rejected")
    void yearAboveMax_alwaysThrowsValidationException(
            @ForAll("yearAboveMax") Integer year) {

        VehicleValidator validator = new VehicleValidator();
        CreateVehicleCommand command = new CreateVehicleCommand("ABC1234", "Toyota", "Corolla", year, null);

        Throwable thrown = catchThrowable(() -> validator.validate(command));

        assertThat(thrown)
                .as("ValidationException must be thrown for year > currentYear + 1")
                .isInstanceOf(ValidationException.class)
                .hasMessage("year must not exceed current year + 1");
    }

    // -----------------------------------------------------------------------
    // Property 8: Year in valid range is always accepted
    // -----------------------------------------------------------------------

    /**
     * Property 8: Year in valid range is always accepted.
     *
     * <p>For any integer year in the range [1886, currentYear + 1] inclusive,
     * {@link VehicleValidator#validate} SHALL NOT throw a {@link ValidationException}
     * for the year field (assuming all other fields are valid).</p>
     *
     * <p>Validates: Requirements 3.3</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-vehicle")
    @Tag("Property_8_Year_in_valid_range_is_always_accepted")
    void validYear_neverThrowsValidationExceptionForYearField(
            @ForAll("validYear") Integer year) {

        VehicleValidator validator = new VehicleValidator();
        CreateVehicleCommand command = new CreateVehicleCommand("ABC1234", "Toyota", "Corolla", year, null);

        Throwable thrown = catchThrowable(() -> validator.validate(command));

        // Either no exception at all, or an exception that is NOT about the year field
        if (thrown != null) {
            assertThat(thrown)
                    .as("Any exception thrown must not be about the year field when year is in valid range")
                    .isInstanceOf(ValidationException.class);
            assertThat(thrown.getMessage())
                    .as("Exception message must not be a year-related message for a valid year")
                    .doesNotContain("year must be 1886 or later")
                    .doesNotContain("year must not exceed current year + 1");
        }
    }

    // -----------------------------------------------------------------------
    // Property 9: Validation failure prevents all repository calls
    // -----------------------------------------------------------------------

    /**
     * Property 9: Validation failure prevents all repository calls.
     *
     * <p>For any {@link CreateVehicleCommand} that fails {@link VehicleValidator#validate}
     * (null/blank plate, null/blank brand, null/blank model, null year, out-of-range year),
     * {@link VehicleService#createVehicle} SHALL NOT call
     * {@code VehicleRepository.existsByPlate} or {@code VehicleRepository.save}.</p>
     *
     * <p>Validates: Requirements 2.8</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-vehicle")
    @Tag("Property_9_Validation_failure_prevents_all_repository_calls")
    void validationFailure_preventsAllRepositoryCalls(
            @ForAll("invalidCommand") CreateVehicleCommand invalidCommand) {

        boolean[] existsByPlateCalled = {false};
        boolean[] saveCalled = {false};

        VehicleRepository vehicleRepository = trackingVehicleRepository(existsByPlateCalled, saveCalled);
        DriverRepository driverRepository = noCallDriverRepository();
        VehicleService vehicleService = new VehicleService(vehicleRepository, driverRepository, new VehicleValidator());

        Throwable thrown = catchThrowable(() -> vehicleService.createVehicle(invalidCommand));

        assertThat(thrown)
                .as("A ValidationException must be thrown for an invalid command")
                .isInstanceOf(ValidationException.class);

        assertThat(existsByPlateCalled[0])
                .as("VehicleRepository.existsByPlate must NOT be called when validation fails")
                .isFalse();

        assertThat(saveCalled[0])
                .as("VehicleRepository.save must NOT be called when validation fails")
                .isFalse();
    }

    /**
     * Produces {@link CreateVehicleCommand} instances that are guaranteed to fail
     * {@link VehicleValidator#validate}. Covers: null/blank plate, null/blank brand,
     * null/blank model, null year, year below 1886, year above currentYear+1.
     */
    @Provide
    Arbitrary<CreateVehicleCommand> invalidCommand() {
        int validYear = Year.now().getValue();
        int tooFarFuture = Year.now().getValue() + 2;

        // null plate
        Arbitrary<CreateVehicleCommand> nullPlate =
                Arbitraries.just(new CreateVehicleCommand(null, "Toyota", "Corolla", validYear, null));

        // blank plate
        Arbitrary<CreateVehicleCommand> blankPlate = Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(1)
                .ofMaxLength(10)
                .map(s -> new CreateVehicleCommand(s, "Toyota", "Corolla", validYear, null));

        // null brand
        Arbitrary<CreateVehicleCommand> nullBrand =
                Arbitraries.just(new CreateVehicleCommand("ABC1234", null, "Corolla", validYear, null));

        // blank brand
        Arbitrary<CreateVehicleCommand> blankBrand = Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(1)
                .ofMaxLength(10)
                .map(s -> new CreateVehicleCommand("ABC1234", s, "Corolla", validYear, null));

        // null model
        Arbitrary<CreateVehicleCommand> nullModel =
                Arbitraries.just(new CreateVehicleCommand("ABC1234", "Toyota", null, validYear, null));

        // blank model
        Arbitrary<CreateVehicleCommand> blankModel = Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(1)
                .ofMaxLength(10)
                .map(s -> new CreateVehicleCommand("ABC1234", "Toyota", s, validYear, null));

        // null year
        Arbitrary<CreateVehicleCommand> nullYear =
                Arbitraries.just(new CreateVehicleCommand("ABC1234", "Toyota", "Corolla", null, null));

        // year below 1886
        Arbitrary<CreateVehicleCommand> yearTooLow = Arbitraries.integers()
                .between(Integer.MIN_VALUE, 1885)
                .map(y -> new CreateVehicleCommand("ABC1234", "Toyota", "Corolla", y, null));

        // year above currentYear + 1
        Arbitrary<CreateVehicleCommand> yearTooHigh = Arbitraries.integers()
                .between(tooFarFuture, Integer.MAX_VALUE)
                .map(y -> new CreateVehicleCommand("ABC1234", "Toyota", "Corolla", y, null));

        return Arbitraries.oneOf(
                nullPlate, blankPlate,
                nullBrand, blankBrand,
                nullModel, blankModel,
                nullYear, yearTooLow, yearTooHigh
        );
    }
}
