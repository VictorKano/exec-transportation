package com.example.fleet.application;

import com.example.fleet.application.command.CreateUserCommand;
import com.example.fleet.application.response.UserResponse;
import com.example.fleet.application.service.UserService;
import com.example.fleet.application.validator.UserValidator;
import com.example.fleet.domain.model.User;
import com.example.fleet.domain.port.PasswordEncoder;
import com.example.fleet.domain.repository.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.lifecycle.BeforeProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link UserService}.
 *
 * <p>Uses jqwik to verify universal correctness properties across a wide input space.
 * No Spring context — pure jqwik + JUnit 5 with in-memory stubs.</p>
 */
class UserServicePropertyTest {

    private UserService userService;

    @BeforeProperty
    void setUp() {
        // In-memory stub: existsByEmail always returns false (no duplicates), save returns user as-is
        UserRepository userRepository = new UserRepository() {
            @Override
            public User save(User user) {
                return user;
            }

            @Override
            public boolean existsByEmail(String email) {
                return false;
            }

            @Override
            public java.util.Optional<User> findByEmail(String email) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<User> findById(java.util.UUID id) {
                return java.util.Optional.empty();
            }
        };

        // Stub: returns "hashed_" + rawPassword
        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(String rawPassword) {
                return "hashed_" + rawPassword;
            }

            @Override
            public boolean matches(String rawPassword, String encodedPassword) {
                return encodedPassword.equals("hashed_" + rawPassword);
            }
        };

        // Real validator — ensures generated inputs are truly valid
        UserValidator userValidator = new UserValidator();

        userService = new UserService(userRepository, passwordEncoder, userValidator);
    }

    // -----------------------------------------------------------------------
    // Generators
    // -----------------------------------------------------------------------

    /** Arbitrary that produces a valid non-blank name (alpha, 1–50 chars). */
    @Provide
    Arbitrary<String> validName() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
    }

    /** Arbitrary that produces a valid RFC 5322-ish email (local@domain.tld). */
    @Provide
    Arbitrary<String> validEmail() {
        Arbitrary<String> local = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        Arbitrary<String> domain = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10);
        Arbitrary<String> tld = Arbitraries.of("com", "org", "net", "io");
        return Combinators.combine(local, domain, tld)
                .as((l, d, t) -> l + "@" + d + "." + t);
    }

    /** Arbitrary that produces a valid password (ascii, 8–30 chars, non-blank). */
    @Provide
    Arbitrary<String> validPassword() {
        return Arbitraries.strings().ascii().ofMinLength(8).ofMaxLength(30)
                .filter(s -> !s.isBlank());
    }

    /** Arbitrary that produces a valid non-blank phone number (7–20 chars from allowed set). */
    @Provide
    Arbitrary<String> validPhoneNumber() {
        return Arbitraries.strings().withChars("+0123456789-() ").ofMinLength(7).ofMaxLength(20)
                .filter(s -> !s.isBlank());
    }

    // -----------------------------------------------------------------------
    // Property 1: Valid registration always produces a response with all required public fields
    // -----------------------------------------------------------------------

    /**
     * Validates: Requirements 1.1, 5.1, 5.3
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_1_Valid_registration_always_produces_a_response_with_all_required_public_fields")
    void validCommand_alwaysReturnsCompleteResponse(
            @ForAll("validName") String name,
            @ForAll("validEmail") String email,
            @ForAll("validPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        CreateUserCommand command = new CreateUserCommand(name, email, password, phoneNumber);

        UserResponse response = userService.createUser(command);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo(command.name());
        assertThat(response.email()).isEqualTo(command.email());
        assertThat(response.phoneNumber()).isEqualTo(command.phoneNumber());
    }

    // -----------------------------------------------------------------------
    // Property 2: Password is never exposed in the response
    // -----------------------------------------------------------------------

    /**
     * For any valid {@link CreateUserCommand}, the {@link UserResponse} returned by
     * {@link UserService#createUser} must contain no field that equals the plain-text
     * password or the BCrypt hash of that password.
     *
     * <p>The stub {@link PasswordEncoder} used in this test produces a deterministic hash
     * of the form {@code "hashed_<password>"}, so both the plain-text value and the
     * hashed value are known and can be checked against every field of the response.
     *
     * <p>Validates: Requirements 4.3
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_2_Password_is_never_exposed_in_the_response")
    void validCommand_passwordNeverExposedInResponse(
            @ForAll("validName") String name,
            @ForAll("validEmail") String email,
            @ForAll("validPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        CreateUserCommand command = new CreateUserCommand(name, email, password, phoneNumber);

        UserResponse response = userService.createUser(command);

        // The stub encoder produces "hashed_<password>" — both forms must be absent
        String hashedPassword = "hashed_" + password;

        assertThat(response.id().toString())
                .as("id must not equal the plain-text password")
                .isNotEqualTo(password)
                .as("id must not equal the hashed password")
                .isNotEqualTo(hashedPassword);

        assertThat(response.name())
                .as("name must not equal the plain-text password")
                .isNotEqualTo(password)
                .as("name must not equal the hashed password")
                .isNotEqualTo(hashedPassword);

        assertThat(response.email())
                .as("email must not equal the plain-text password")
                .isNotEqualTo(password)
                .as("email must not equal the hashed password")
                .isNotEqualTo(hashedPassword);

        assertThat(response.phoneNumber())
                .as("phoneNumber must not equal the plain-text password")
                .isNotEqualTo(password)
                .as("phoneNumber must not equal the hashed password")
                .isNotEqualTo(hashedPassword);
    }

    // -----------------------------------------------------------------------
    // Property 7: Every successfully created user receives a unique UUID
    // -----------------------------------------------------------------------

    /**
     * For any two distinct valid {@link CreateUserCommand} objects with different emails,
     * the {@code id} values in their respective {@link UserResponse} objects SHALL be
     * different UUIDs.
     *
     * <p>Validates: Requirements 5.1, 5.2
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_7_Every_successfully_created_user_receives_a_unique_UUID")
    void twoDistinctCommands_alwaysReceiveDifferentUUIDs(
            @ForAll("validName") String name1,
            @ForAll("validEmail") String email1,
            @ForAll("validPassword") String password1,
            @ForAll("validPhoneNumber") String phoneNumber1,
            @ForAll("validName") String name2,
            @ForAll("validEmail") String email2,
            @ForAll("validPassword") String password2,
            @ForAll("validPhoneNumber") String phoneNumber2) {

        // Ensure the two commands have different emails so both succeed
        net.jqwik.api.Assume.that(!email1.equals(email2));

        CreateUserCommand command1 = new CreateUserCommand(name1, email1, password1, phoneNumber1);
        CreateUserCommand command2 = new CreateUserCommand(name2, email2, password2, phoneNumber2);

        UserResponse response1 = userService.createUser(command1);
        UserResponse response2 = userService.createUser(command2);

        assertThat(response1.id())
                .as("Two distinct users must receive different UUIDs")
                .isNotEqualTo(response2.id());
    }

    // -----------------------------------------------------------------------
    // Property 6: Duplicate email is always rejected
    // -----------------------------------------------------------------------

    /**
     * For any valid {@link CreateUserCommand} whose email is already present in the
     * repository, {@link UserService#createUser} must throw {@link com.example.fleet.application.exception.DuplicateEmailException}
     * and must NOT call {@link UserRepository#save}.
     *
     * <p>Validates: Requirements 3.1
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_6_Duplicate_email_is_always_rejected")
    void duplicateEmail_alwaysThrowsDuplicateEmailException(
            @ForAll("validName") String name,
            @ForAll("validEmail") String email,
            @ForAll("validPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        // Track whether save was called
        boolean[] saveCalled = {false};

        // Stub: existsByEmail returns true for the command's email (pre-seeded duplicate)
        UserRepository duplicateRepository = new UserRepository() {
            @Override
            public User save(User user) {
                saveCalled[0] = true;
                return user;
            }

            @Override
            public boolean existsByEmail(String emailArg) {
                return emailArg.equals(email);
            }

            @Override
            public java.util.Optional<User> findByEmail(String emailArg) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<User> findById(java.util.UUID id) {
                return java.util.Optional.empty();
            }
        };

        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(String rawPassword) {
                return "hashed_" + rawPassword;
            }

            @Override
            public boolean matches(String rawPassword, String encodedPassword) {
                return encodedPassword.equals("hashed_" + rawPassword);
            }
        };
        UserValidator userValidator = new UserValidator();
        UserService serviceWithDuplicate = new UserService(duplicateRepository, passwordEncoder, userValidator);

        CreateUserCommand command = new CreateUserCommand(name, email, password, phoneNumber);

        // Assert DuplicateEmailException is thrown
        org.assertj.core.api.ThrowableAssert.ThrowingCallable call =
                () -> serviceWithDuplicate.createUser(command);
        assertThat(org.assertj.core.api.Assertions.catchThrowable(call))
                .as("DuplicateEmailException must be thrown for a pre-existing email")
                .isInstanceOf(com.example.fleet.application.exception.DuplicateEmailException.class);

        // Assert save was never called
        assertThat(saveCalled[0])
                .as("userRepository.save must never be called when email is a duplicate")
                .isFalse();
    }

    // -----------------------------------------------------------------------
    // Property 8: Password is stored only in hashed form (BCrypt round-trip)
    // -----------------------------------------------------------------------

    /**
     * For any plain-text password, after {@link UserService#createUser} persists the user,
     * the value stored in the repository SHALL be a BCrypt hash (verifiable via
     * {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder#matches})
     * and SHALL NOT equal the plain-text password.
     *
     * <p>This test uses a real {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}
     * to ensure the hash is valid and can be verified. The test captures the {@link User}
     * object passed to {@link UserRepository#save} and asserts:
     * <ul>
     *   <li>The {@code hashedPassword} field is a valid BCrypt hash (starts with "$2a$" or "$2b$")</li>
     *   <li>The {@code hashedPassword} does NOT equal the plain-text password</li>
     *   <li>The BCrypt encoder can successfully match the plain-text password against the hash</li>
     * </ul>
     *
     * <p>Validates: Requirements 4.1, 4.2
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_8_Password_is_stored_only_in_hashed_form")
    void passwordStoredAsValidBCryptHash(
            @ForAll("validName") String name,
            @ForAll("validEmail") String email,
            @ForAll("validPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        // Use a real BCryptPasswordEncoder to verify the hash is valid
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder bcryptEncoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        // Capture the User object passed to save
        User[] capturedUser = {null};

        UserRepository capturingRepository = new UserRepository() {
            @Override
            public User save(User user) {
                capturedUser[0] = user;
                return user;
            }

            @Override
            public boolean existsByEmail(String emailArg) {
                return false;
            }

            @Override
            public java.util.Optional<User> findByEmail(String emailArg) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<User> findById(java.util.UUID id) {
                return java.util.Optional.empty();
            }
        };

        // Wrap the real BCrypt encoder in the domain interface
        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(String rawPassword) {
                return bcryptEncoder.encode(rawPassword);
            }

            @Override
            public boolean matches(String rawPassword, String encodedPassword) {
                return bcryptEncoder.matches(rawPassword, encodedPassword);
            }
        };

        UserValidator userValidator = new UserValidator();
        UserService serviceWithRealBCrypt = new UserService(capturingRepository, passwordEncoder, userValidator);

        CreateUserCommand command = new CreateUserCommand(name, email, password, phoneNumber);

        // Execute the service method
        serviceWithRealBCrypt.createUser(command);

        // Assert the User was captured
        assertThat(capturedUser[0])
                .as("User must be passed to repository.save")
                .isNotNull();

        String hashedPassword = capturedUser[0].getHashedPassword();

        // Assert the hashed password is not null
        assertThat(hashedPassword)
                .as("hashedPassword must not be null")
                .isNotNull();

        // Assert the hashed password does NOT equal the plain-text password
        assertThat(hashedPassword)
                .as("hashedPassword must NOT equal the plain-text password")
                .isNotEqualTo(password);

        // Assert the hashed password is a valid BCrypt hash (starts with $2a$ or $2b$)
        assertThat(hashedPassword)
                .as("hashedPassword must be a valid BCrypt hash")
                .matches("^\\$2[ab]\\$\\d{2}\\$.{53}$");

        // Assert the BCrypt encoder can match the plain-text password against the hash
        assertThat(bcryptEncoder.matches(password, hashedPassword))
                .as("BCrypt encoder must be able to verify the plain-text password against the hash")
                .isTrue();
    }
}
