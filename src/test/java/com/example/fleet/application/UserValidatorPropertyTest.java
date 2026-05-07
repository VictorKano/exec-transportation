package com.example.fleet.application;

import com.example.fleet.application.command.CreateUserCommand;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.validator.UserValidator;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.lifecycle.BeforeProperty;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for {@link UserValidator}.
 *
 * <p>Uses jqwik to verify universal correctness properties across a wide input space.</p>
 */
class UserValidatorPropertyTest {

    private UserValidator validator;

    @BeforeProperty
    void setUp() {
        validator = new UserValidator();
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

    /** Arbitrary that produces a valid non-blank name. */
    @Provide
    Arbitrary<String> validName() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
    }

    /** Arbitrary that produces a valid RFC 5322-ish email. */
    @Provide
    Arbitrary<String> validEmail() {
        Arbitrary<String> local = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        Arbitrary<String> domain = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10);
        Arbitrary<String> tld = Arbitraries.of("com", "org", "net", "io");
        return Combinators.combine(local, domain, tld)
                .as((l, d, t) -> l + "@" + d + "." + t);
    }

    /** Arbitrary that produces a valid password (≥ 8 non-blank characters). */
    @Provide
    Arbitrary<String> validPassword() {
        return Arbitraries.strings().ascii().ofMinLength(8).ofMaxLength(30)
                .filter(s -> !s.isBlank());
    }

    /** Arbitrary that produces a valid non-blank phone number. */
    @Provide
    Arbitrary<String> validPhoneNumber() {
        return Arbitraries.strings().withChars("+0123456789-() ").ofMinLength(7).ofMaxLength(20)
                .filter(s -> !s.isBlank());
    }

    // -----------------------------------------------------------------------
    // Property 3: Whitespace-only or blank required fields are always rejected
    // -----------------------------------------------------------------------

    /**
     * Validates: Requirements 2.1
     * Tag: Feature: create-user, Property 3: Whitespace-only or blank required fields are always rejected
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_3_Whitespace-only_or_blank_required_fields_are_always_rejected")
    void blankName_alwaysThrowsValidationException(
            @ForAll("blankOrNull") String blankName,
            @ForAll("validEmail") String email,
            @ForAll("validPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        CreateUserCommand command = new CreateUserCommand(blankName, email, password, phoneNumber);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("name is required");
    }

    /**
     * Validates: Requirements 2.2
     * Tag: Feature: create-user, Property 3: Whitespace-only or blank required fields are always rejected
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_3_Whitespace-only_or_blank_required_fields_are_always_rejected")
    void blankEmail_alwaysThrowsValidationException(
            @ForAll("validName") String name,
            @ForAll("blankOrNull") String blankEmail,
            @ForAll("validPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        CreateUserCommand command = new CreateUserCommand(name, blankEmail, password, phoneNumber);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("email is required");
    }

    /**
     * Validates: Requirements 2.3
     * Tag: Feature: create-user, Property 3: Whitespace-only or blank required fields are always rejected
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_3_Whitespace-only_or_blank_required_fields_are_always_rejected")
    void blankPassword_alwaysThrowsValidationException(
            @ForAll("validName") String name,
            @ForAll("validEmail") String email,
            @ForAll("blankOrNull") String blankPassword,
            @ForAll("validPhoneNumber") String phoneNumber) {

        CreateUserCommand command = new CreateUserCommand(name, email, blankPassword, phoneNumber);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("password is required");
    }

    /**
     * Validates: Requirements 2.4
     * Tag: Feature: create-user, Property 3: Whitespace-only or blank required fields are always rejected
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_3_Whitespace-only_or_blank_required_fields_are_always_rejected")
    void blankPhoneNumber_alwaysThrowsValidationException(
            @ForAll("validName") String name,
            @ForAll("validEmail") String email,
            @ForAll("validPassword") String password,
            @ForAll("blankOrNull") String blankPhoneNumber) {

        CreateUserCommand command = new CreateUserCommand(name, email, password, blankPhoneNumber);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("phoneNumber is required");
    }

    // -----------------------------------------------------------------------
    // Property 4: Invalid email format is always rejected
    // -----------------------------------------------------------------------

    @Provide
    Arbitrary<String> noAtSign() {
        return Arbitraries.strings()
                .withCharRange(' ', '~')
                .ofMinLength(1)
                .ofMaxLength(30)
                .filter(s -> !s.contains("@") && !s.isBlank());
    }

    @Provide
    Arbitrary<String> multipleAtSigns() {
        Arbitrary<String> part = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        return Combinators.combine(part, part, part)
                .as((a, b, c) -> a + "@" + b + "@" + c);
    }

    @Provide
    Arbitrary<String> missingDomainDot() {
        Arbitrary<String> local = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        Arbitrary<String> domainNoDot = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        return Combinators.combine(local, domainNoDot)
                .as((l, d) -> l + "@" + d);
    }

    @Provide
    Arbitrary<String> trailingDotDomain() {
        Arbitrary<String> local = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        Arbitrary<String> domain = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10);
        return Combinators.combine(local, domain)
                .as((l, d) -> l + "@" + d + ".");
    }

    @Provide
    Arbitrary<String> missingLocalPart() {
        Arbitrary<String> domain = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10);
        Arbitrary<String> tld = Arbitraries.of("com", "org", "net");
        return Combinators.combine(domain, tld)
                .as((d, t) -> "@" + d + "." + t);
    }

    /**
     * Validates: Requirements 2.5
     * Tag: Feature: create-user, Property 4: Invalid email format is always rejected
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_4_Invalid_email_format_is_always_rejected")
    void emailWithNoAtSign_alwaysThrowsValidationException(
            @ForAll("validName") String name,
            @ForAll("noAtSign") String invalidEmail,
            @ForAll("validPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        CreateUserCommand command = new CreateUserCommand(name, invalidEmail, password, phoneNumber);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("email format is invalid");
    }

    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_4_Invalid_email_format_is_always_rejected")
    void emailWithMultipleAtSigns_alwaysThrowsValidationException(
            @ForAll("validName") String name,
            @ForAll("multipleAtSigns") String invalidEmail,
            @ForAll("validPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        CreateUserCommand command = new CreateUserCommand(name, invalidEmail, password, phoneNumber);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("email format is invalid");
    }

    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_4_Invalid_email_format_is_always_rejected")
    void emailWithNoDotInDomain_alwaysThrowsValidationException(
            @ForAll("validName") String name,
            @ForAll("missingDomainDot") String invalidEmail,
            @ForAll("validPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        CreateUserCommand command = new CreateUserCommand(name, invalidEmail, password, phoneNumber);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("email format is invalid");
    }

    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_4_Invalid_email_format_is_always_rejected")
    void emailWithTrailingDot_alwaysThrowsValidationException(
            @ForAll("validName") String name,
            @ForAll("trailingDotDomain") String invalidEmail,
            @ForAll("validPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        CreateUserCommand command = new CreateUserCommand(name, invalidEmail, password, phoneNumber);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("email format is invalid");
    }

    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_4_Invalid_email_format_is_always_rejected")
    void emailWithMissingLocalPart_alwaysThrowsValidationException(
            @ForAll("validName") String name,
            @ForAll("missingLocalPart") String invalidEmail,
            @ForAll("validPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        CreateUserCommand command = new CreateUserCommand(name, invalidEmail, password, phoneNumber);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("email format is invalid");
    }

    // -----------------------------------------------------------------------
    // Property 5: Short passwords are always rejected
    // -----------------------------------------------------------------------

    @Provide
    Arbitrary<String> shortPassword() {
        return Arbitraries.strings()
                .ascii()
                .ofMinLength(0)
                .ofMaxLength(7);
    }

    /**
     * Validates: Requirements 2.6
     * Tag: Feature: create-user, Property 5: Short passwords are always rejected
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_5_Short_passwords_are_always_rejected")
    void shortPassword_alwaysThrowsValidationException(
            @ForAll("validName") String name,
            @ForAll("validEmail") String email,
            @ForAll("shortPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        Assume.that(password != null && !password.isBlank());

        CreateUserCommand command = new CreateUserCommand(name, email, password, phoneNumber);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("password must be at least 8 characters");
    }

    /**
     * Validates: Requirements 2.6 (edge case: empty string)
     * Tag: Feature: create-user, Property 5: Short passwords are always rejected
     */
    @Property(tries = 100)
    @Tag("Feature_create-user")
    @Tag("Property_5_Short_passwords_are_always_rejected")
    void emptyOrBlankShortPassword_alwaysThrowsValidationException(
            @ForAll("validName") String name,
            @ForAll("validEmail") String email,
            @ForAll("shortPassword") String password,
            @ForAll("validPhoneNumber") String phoneNumber) {

        Assume.that(password == null || password.isBlank());

        CreateUserCommand command = new CreateUserCommand(name, email, password, phoneNumber);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class);
    }
}
