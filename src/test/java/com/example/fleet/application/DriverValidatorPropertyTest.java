package com.example.fleet.application;

import com.example.fleet.application.command.CreateDriverCommand;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.validator.DriverValidator;
import com.example.fleet.domain.model.DriverStatus;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for {@link DriverValidator}.
 *
 * <p>Uses jqwik to verify universal correctness properties across a wide input space.</p>
 *
 * <p>Requirements covered: 2.1, 2.2, 2.3, 4.1, 5.1, 11.1</p>
 */
class DriverValidatorPropertyTest {

    private DriverValidator validator;

    @BeforeProperty
    void setUp() {
        validator = new DriverValidator();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Arbitrary that produces null or an all-whitespace string. */
    @Provide
    Arbitrary<String> blankOrNull() {
        Arbitrary<String> nullArb = Arbitraries.just(null);
        Arbitrary<String> whitespaceArb = Arbitraries
                .strings()
                .withChars(' ', '\t', '\n', '\r', '\f')
                .ofMinLength(1)
                .ofMaxLength(20);
        return Arbitraries.oneOf(nullArb, whitespaceArb);
    }

    /** Arbitrary that produces a valid alphanumeric CNH (1-20 characters). */
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

    /** Arbitrary that produces a non-alphanumeric string (contains at least one special character). */
    @Provide
    Arbitrary<String> nonAlphanumericCnh() {
        Arbitrary<String> alphanumericPart = Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(0)
                .ofMaxLength(10);
        
        Arbitrary<Character> specialChar = Arbitraries.of('-', '_', '@', '!', '#', '$', '%', ' ', '.', '/');
        
        return Combinators.combine(alphanumericPart, specialChar, alphanumericPart)
                .as((prefix, special, suffix) -> prefix + special + suffix)
                .filter(s -> !s.isBlank());
    }

    /** Arbitrary that produces an alphanumeric string with length > 20. */
    @Provide
    Arbitrary<String> tooLongCnh() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(21)
                .ofMaxLength(50);
    }

    // -----------------------------------------------------------------------
    // Property 1: Blank or null CNH is always rejected
    // -----------------------------------------------------------------------

    /**
     * Property 1: Blank or null CNH is always rejected.
     *
     * <p>Validates: Requirements 2.1</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-driver")
    @Tag("Property_1_Blank_or_null_CNH_is_always_rejected")
    void blankOrNullCnh_alwaysThrowsValidationException(
            @ForAll("validUserId") UUID userId,
            @ForAll("blankOrNull") String blankCnh,
            @ForAll("validStatus") DriverStatus status) {

        CreateDriverCommand command = new CreateDriverCommand(userId, blankCnh, status);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cnh is required");
    }

    // -----------------------------------------------------------------------
    // Property 2: Non-alphanumeric CNH is always rejected
    // -----------------------------------------------------------------------

    /**
     * Property 2: Non-alphanumeric CNH is always rejected.
     *
     * <p>Validates: Requirements 2.2</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-driver")
    @Tag("Property_2_Non-alphanumeric_CNH_is_always_rejected")
    void nonAlphanumericCnh_alwaysThrowsValidationException(
            @ForAll("validUserId") UUID userId,
            @ForAll("nonAlphanumericCnh") String invalidCnh,
            @ForAll("validStatus") DriverStatus status) {

        CreateDriverCommand command = new CreateDriverCommand(userId, invalidCnh, status);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cnh must be alphanumeric");
    }

    // -----------------------------------------------------------------------
    // Property 3: Out-of-range CNH length is always rejected
    // -----------------------------------------------------------------------

    /**
     * Property 3: Out-of-range CNH length is always rejected.
     *
     * <p>Validates: Requirements 2.3</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-driver")
    @Tag("Property_3_Out-of-range_CNH_length_is_always_rejected")
    void tooLongCnh_alwaysThrowsValidationException(
            @ForAll("validUserId") UUID userId,
            @ForAll("tooLongCnh") String longCnh,
            @ForAll("validStatus") DriverStatus status) {

        CreateDriverCommand command = new CreateDriverCommand(userId, longCnh, status);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cnh must be between 1 and 20 characters");
    }

    // -----------------------------------------------------------------------
    // Property 4: Null userId is always rejected
    // -----------------------------------------------------------------------

    /**
     * Property 4: Null userId is always rejected.
     *
     * <p>Validates: Requirements 5.1</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-driver")
    @Tag("Property_4_Null_userId_is_always_rejected")
    void nullUserId_alwaysThrowsValidationException(
            @ForAll("validCnh") String cnh,
            @ForAll("validStatus") DriverStatus status) {

        CreateDriverCommand command = new CreateDriverCommand(null, cnh, status);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("userId is required");
    }

    // -----------------------------------------------------------------------
    // Property 5: Null status is always rejected
    // -----------------------------------------------------------------------

    /**
     * Property 5: Null status is always rejected.
     *
     * <p>Validates: Requirements 4.1</p>
     */
    @Property(tries = 100)
    @Tag("Feature_create-driver")
    @Tag("Property_5_Null_status_is_always_rejected")
    void nullStatus_alwaysThrowsValidationException(
            @ForAll("validUserId") UUID userId,
            @ForAll("validCnh") String cnh) {

        CreateDriverCommand command = new CreateDriverCommand(userId, cnh, null);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("status is required");
    }
}
