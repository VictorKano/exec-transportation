package com.example.fleet.application;

import com.example.fleet.application.command.CreateDriverCommand;
import com.example.fleet.application.exception.DuplicateCnhException;
import com.example.fleet.application.exception.UserNotFoundException;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.response.DriverResponse;
import com.example.fleet.application.service.DriverService;
import com.example.fleet.application.validator.DriverValidator;
import com.example.fleet.domain.model.Driver;
import com.example.fleet.domain.model.DriverStatus;
import com.example.fleet.domain.model.User;
import com.example.fleet.domain.repository.DriverRepository;
import com.example.fleet.domain.repository.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Property-based tests for {@link DriverService}.
 *
 * <p>Uses jqwik to verify universal correctness properties across a wide input space.
 * No Spring context — pure jqwik + JUnit 5 with in-memory stubs.</p>
 *
 * <p>Requirements covered: 1.1, 1.2, 1.3, 3.1, 3.2, 3.3, 5.2, 6.4, 11.3, 11.4</p>
 */
class DriverServicePropertyTest {

    // -----------------------------------------------------------------------
    // Generators
    // -----------------------------------------------------------------------

    /** Arbitrary that produces a valid alphanumeric CNH (1–20 characters). */
    @Provide
    Arbitrary<String> validCnh() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    /** Arbitrary that produces a valid UUID. */
    @Provide
    Arbitrary<UUID> validUserId() {
        return Arbitraries.create(UUID::randomUUID);
    }

    /** Arbitrary that produces a valid DriverStatus. */
    @Provide
    Arbitrary<DriverStatus> validStatus() {
        return Arbitraries.of(DriverStatus.ACTIVE, DriverStatus.INACTIVE);
    }

    // -----------------------------------------------------------------------
    // Stub factories
    // -----------------------------------------------------------------------

    /**
     * Builds a {@link UserRepository} stub where {@code findById} always returns empty
     * (simulating an unknown user). {@code save} tracks whether it was called.
     */
    private UserRepository unknownUserRepository() {
        return new UserRepository() {
            @Override
            public User save(User user) { return user; }

            @Override
            public boolean existsByEmail(String email) { return false; }

            @Override
            public Optional<User> findByEmail(String email) { return Optional.empty(); }

            @Override
            public Optional<User> findById(UUID id) { return Optional.empty(); }
        };
    }

    /**
     * Builds a {@link UserRepository} stub where {@code findById} always returns a
     * synthetic {@link User} for any id.
     */
    private UserRepository knownUserRepository() {
        return new UserRepository() {
            @Override
            public User save(User user) { return user; }

            @Override
            public boolean existsByEmail(String email) { return false; }

            @Override
            public Optional<User> findByEmail(String email) { return Optional.empty(); }

            @Override
            public Optional<User> findById(UUID id) {
                return Optional.of(new User(id, "Test User", "test@example.com", "hashed", "+1-555-0100"));
            }
        };
    }

    /**
     * Builds a {@link DriverRepository} stub where {@code existsByCnh} always returns
     * {@code true} (simulating a pre-existing duplicate CNH). Tracks whether {@code save}
     * was called via the provided boolean array.
     */
    private DriverRepository duplicateCnhRepository(boolean[] saveCalled) {
        return new DriverRepository() {
            @Override
            public Driver save(Driver driver) {
                saveCalled[0] = true;
                return driver;
            }

            @Override
            public boolean existsByCnh(String cnh) { return true; }

            @Override
            public Optional<Driver> findById(UUID id) { return Optional.empty(); }
        };
    }

    /**
     * Builds a {@link DriverRepository} stub where {@code existsByCnh} always returns
     * {@code false} and {@code save} returns the driver as-is.
     */
    private DriverRepository freshDriverRepository() {
        return new DriverRepository() {
            @Override
            public Driver save(Driver driver) { return driver; }

            @Override
            public boolean existsByCnh(String cnh) { return false; }

            @Override
            public Optional<Driver> findById(UUID id) { return Optional.empty(); }
        };
    }

    /**
     * Builds a {@link DriverRepository} stub that tracks whether {@code save} was called.
     */
    private DriverRepository trackingSaveRepository(boolean[] saveCalled) {
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

    // -----------------------------------------------------------------------
    // Property 6: Unknown userId always throws UserNotFoundException
    // -----------------------------------------------------------------------

    /**
     * Property 6: Unknown userId always throws UserNotFoundException.
     *
     * <p>For any valid {@link CreateDriverCommand} where {@code UserRepository.findById}
     * returns {@code Optional.empty()}, {@link DriverService#createDriver} SHALL throw a
     * {@link UserNotFoundException} and SHALL NOT call {@code DriverRepository.save}.</p>
     *
     * <p>Validates: Requirements 1.1, 1.2</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-driver")
    @Tag("Property_6_Unknown_userId_always_throws_UserNotFoundException")
    void unknownUserId_alwaysThrowsUserNotFoundException(
            @ForAll("validUserId") UUID userId,
            @ForAll("validCnh") String cnh,
            @ForAll("validStatus") DriverStatus status) {

        boolean[] saveCalled = {false};

        UserRepository userRepository = unknownUserRepository();
        DriverRepository driverRepository = trackingSaveRepository(saveCalled);
        DriverService driverService = new DriverService(userRepository, driverRepository, new DriverValidator());

        CreateDriverCommand command = new CreateDriverCommand(userId, cnh, status);

        Throwable thrown = catchThrowable(() -> driverService.createDriver(command));

        assertThat(thrown)
                .as("UserNotFoundException must be thrown when userId is not found")
                .isInstanceOf(UserNotFoundException.class);

        assertThat(saveCalled[0])
                .as("DriverRepository.save must NOT be called when user is not found")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Property 7: Duplicate CNH always throws DuplicateCnhException without saving
    // -----------------------------------------------------------------------

    /**
     * Property 7: Duplicate CNH always throws DuplicateCnhException without saving.
     *
     * <p>For any valid {@link CreateDriverCommand} where {@code DriverRepository.existsByCnh}
     * returns {@code true}, {@link DriverService#createDriver} SHALL throw a
     * {@link DuplicateCnhException} and SHALL NOT call {@code DriverRepository.save}.</p>
     *
     * <p>Validates: Requirements 3.1, 3.2, 3.3</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-driver")
    @Tag("Property_7_Duplicate_CNH_always_throws_DuplicateCnhException_without_saving")
    void duplicateCnh_alwaysThrowsDuplicateCnhExceptionWithoutSaving(
            @ForAll("validUserId") UUID userId,
            @ForAll("validCnh") String cnh,
            @ForAll("validStatus") DriverStatus status) {

        boolean[] saveCalled = {false};

        UserRepository userRepository = knownUserRepository();
        DriverRepository driverRepository = duplicateCnhRepository(saveCalled);
        DriverService driverService = new DriverService(userRepository, driverRepository, new DriverValidator());

        CreateDriverCommand command = new CreateDriverCommand(userId, cnh, status);

        Throwable thrown = catchThrowable(() -> driverService.createDriver(command));

        assertThat(thrown)
                .as("DuplicateCnhException must be thrown when CNH already exists")
                .isInstanceOf(DuplicateCnhException.class);

        assertThat(saveCalled[0])
                .as("DriverRepository.save must NOT be called when CNH is a duplicate")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Property 8: Valid command always produces a correct DriverResponse
    // -----------------------------------------------------------------------

    /**
     * Property 8: Valid command always produces a correct DriverResponse.
     *
     * <p>For any valid {@link CreateDriverCommand} (non-null userId, alphanumeric cnh of
     * 1–20 chars, non-null status) where the user exists and the CNH is not a duplicate,
     * {@link DriverService#createDriver} SHALL return a {@link DriverResponse} where:
     * <ul>
     *   <li>{@code id} is non-null</li>
     *   <li>{@code userId} equals {@code command.userId()}</li>
     *   <li>{@code cnh} equals {@code command.cnh()}</li>
     *   <li>{@code status} equals {@code command.status()}</li>
     * </ul>
     *
     * <p>Validates: Requirements 1.3, 1.4, 4.2, 4.3, 6.1, 6.2, 6.3, 11.3</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-driver")
    @Tag("Property_8_Valid_command_always_produces_a_correct_DriverResponse")
    void validCommand_alwaysProducesCorrectDriverResponse(
            @ForAll("validUserId") UUID userId,
            @ForAll("validCnh") String cnh,
            @ForAll("validStatus") DriverStatus status) {

        UserRepository userRepository = knownUserRepository();
        DriverRepository driverRepository = freshDriverRepository();
        DriverService driverService = new DriverService(userRepository, driverRepository, new DriverValidator());

        CreateDriverCommand command = new CreateDriverCommand(userId, cnh, status);

        DriverResponse response = driverService.createDriver(command);

        assertThat(response.id())
                .as("DriverResponse.id must be non-null")
                .isNotNull();

        assertThat(response.userId())
                .as("DriverResponse.userId must equal command.userId()")
                .isEqualTo(userId);

        assertThat(response.cnh())
                .as("DriverResponse.cnh must equal command.cnh()")
                .isEqualTo(cnh);

        assertThat(response.status())
                .as("DriverResponse.status must equal command.status()")
                .isEqualTo(status);
    }

    // -----------------------------------------------------------------------
    // Property 9: Two valid commands with different CNHs produce different UUIDs
    // -----------------------------------------------------------------------

    /**
     * Property 9: Two valid commands with different CNHs produce different UUIDs.
     *
     * <p>For any two valid {@link CreateDriverCommand} objects with different {@code cnh}
     * values, the {@link DriverResponse} objects returned by
     * {@link DriverService#createDriver} SHALL have different {@code id} values.</p>
     *
     * <p>Validates: Requirements 6.4, 11.4</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-driver")
    @Tag("Property_9_Two_valid_commands_with_different_CNHs_produce_different_UUIDs")
    void twoCommandsWithDifferentCnhs_alwaysProduceDifferentIds(
            @ForAll("validUserId") UUID userId1,
            @ForAll("validCnh") String cnh1,
            @ForAll("validStatus") DriverStatus status1,
            @ForAll("validUserId") UUID userId2,
            @ForAll("validCnh") String cnh2,
            @ForAll("validStatus") DriverStatus status2) {

        // Ensure the two CNHs are different so both commands can succeed
        Assume.that(!cnh1.equals(cnh2));

        UserRepository userRepository = knownUserRepository();
        DriverRepository driverRepository = freshDriverRepository();
        DriverService driverService = new DriverService(userRepository, driverRepository, new DriverValidator());

        CreateDriverCommand command1 = new CreateDriverCommand(userId1, cnh1, status1);
        CreateDriverCommand command2 = new CreateDriverCommand(userId2, cnh2, status2);

        DriverResponse response1 = driverService.createDriver(command1);
        DriverResponse response2 = driverService.createDriver(command2);

        assertThat(response1.id())
                .as("Two drivers created with different CNHs must receive different UUIDs")
                .isNotEqualTo(response2.id());
    }

    // -----------------------------------------------------------------------
    // Property 10: Validation failure prevents all repository calls
    // -----------------------------------------------------------------------

    /**
     * Property 10: Validation failure prevents all repository calls.
     *
     * <p>For any {@link CreateDriverCommand} that fails {@link DriverValidator#validate}
     * (null userId, blank/invalid cnh, or null status), {@link DriverService#createDriver}
     * SHALL NOT call {@code UserRepository.findById} or {@code DriverRepository.save}.</p>
     *
     * <p>Validates: Requirements 5.2</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-driver")
    @Tag("Property_10_Validation_failure_prevents_all_repository_calls")
    void validationFailure_preventsAllRepositoryCalls(
            @ForAll("invalidCommand") CreateDriverCommand invalidCommand) {

        boolean[] findByIdCalled = {false};
        boolean[] saveCalled = {false};

        UserRepository userRepository = new UserRepository() {
            @Override
            public User save(User user) { return user; }

            @Override
            public boolean existsByEmail(String email) { return false; }

            @Override
            public Optional<User> findByEmail(String email) { return Optional.empty(); }

            @Override
            public Optional<User> findById(UUID id) {
                findByIdCalled[0] = true;
                return Optional.empty();
            }
        };

        DriverRepository driverRepository = new DriverRepository() {
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

        DriverService driverService = new DriverService(userRepository, driverRepository, new DriverValidator());

        Throwable thrown = catchThrowable(() -> driverService.createDriver(invalidCommand));

        assertThat(thrown)
                .as("A ValidationException must be thrown for an invalid command")
                .isInstanceOf(ValidationException.class);

        assertThat(findByIdCalled[0])
                .as("UserRepository.findById must NOT be called when validation fails")
                .isFalse();

        assertThat(saveCalled[0])
                .as("DriverRepository.save must NOT be called when validation fails")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Generator for invalid commands (Property 10)
    // -----------------------------------------------------------------------

    /**
     * Produces {@link CreateDriverCommand} instances that are guaranteed to fail
     * {@link DriverValidator#validate}. Covers: null userId, null/blank cnh,
     * non-alphanumeric cnh, cnh longer than 20 chars, and null status.
     */
    @Provide
    Arbitrary<CreateDriverCommand> invalidCommand() {
        UUID validId = UUID.randomUUID();
        String validCnh = "ABC123";
        DriverStatus validStatus = DriverStatus.ACTIVE;

        // null userId
        Arbitrary<CreateDriverCommand> nullUserId =
                Arbitraries.just(new CreateDriverCommand(null, validCnh, validStatus));

        // null cnh
        Arbitrary<CreateDriverCommand> nullCnh =
                Arbitraries.just(new CreateDriverCommand(validId, null, validStatus));

        // blank cnh
        Arbitrary<CreateDriverCommand> blankCnh = Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(1)
                .ofMaxLength(10)
                .map(s -> new CreateDriverCommand(validId, s, validStatus));

        // non-alphanumeric cnh (inject a special character)
        Arbitrary<CreateDriverCommand> nonAlphanumericCnh = Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(1)
                .ofMaxLength(5)
                .flatMap(prefix -> Arbitraries.of('-', '_', '@', '!', '#', '%', '.', '/')
                        .map(special -> new CreateDriverCommand(validId, prefix + special, validStatus)));

        // cnh longer than 20 chars
        Arbitrary<CreateDriverCommand> tooLongCnh = Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(21)
                .ofMaxLength(50)
                .map(s -> new CreateDriverCommand(validId, s, validStatus));

        // null status
        Arbitrary<CreateDriverCommand> nullStatus =
                Arbitraries.just(new CreateDriverCommand(validId, validCnh, null));

        return Arbitraries.oneOf(nullUserId, nullCnh, blankCnh, nonAlphanumericCnh, tooLongCnh, nullStatus);
    }
}
