package com.example.fleet.application;

import com.example.fleet.application.command.LoginCommand;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.validator.CredentialValidator;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for {@link CredentialValidator}.
 *
 * <p>Uses jqwik to verify universal correctness properties across a wide input space.</p>
 *
 * <ul>
 *   <li>Property 4: Blank email is always rejected by CredentialValidator</li>
 *   <li>Property 5: Blank password is always rejected by CredentialValidator (optional)</li>
 * </ul>
 */
class CredentialValidatorPropertyTest {

    private CredentialValidator validator;

    @BeforeProperty
    void setUp() {
        validator = new CredentialValidator();
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

    /** Arbitrary that produces a valid non-blank password. */
    @Provide
    Arbitrary<String> validPassword() {
        return Arbitraries.strings().ascii().ofMinLength(1).ofMaxLength(30)
                .filter(s -> !s.isBlank());
    }

    /** Arbitrary that produces a valid non-blank email. */
    @Provide
    Arbitrary<String> validEmail() {
        Arbitrary<String> local = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        Arbitrary<String> domain = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10);
        Arbitrary<String> tld = Arbitraries.of("com", "org", "net", "io");
        return Combinators.combine(local, domain, tld)
                .as((l, d, t) -> l + "@" + d + "." + t);
    }

    // -----------------------------------------------------------------------
    // Property 4: Blank email is always rejected by CredentialValidator
    // -----------------------------------------------------------------------

    /**
     * For any string that is null or composed entirely of whitespace characters,
     * passing it as the {@code email} field of a {@link LoginCommand} to
     * {@link CredentialValidator#validate} SHALL throw a {@link ValidationException}
     * with a message containing "email is required".
     *
     * <p>Validates: Requirements 2.1</p>
     * Tag: Feature: user-authentication, Property 4: Blank email is always rejected by CredentialValidator
     */
    @Property(tries = 100)
    @Tag("Feature_user-authentication")
    @Tag("Property_4_Blank_email_is_always_rejected_by_CredentialValidator")
    void blankEmail_alwaysThrowsValidationException(
            @ForAll("blankOrNull") String blankEmail,
            @ForAll("validPassword") String password) {

        LoginCommand command = new LoginCommand(blankEmail, password);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("email is required");
    }

    // -----------------------------------------------------------------------
    // Property 5: Blank password is always rejected by CredentialValidator
    // -----------------------------------------------------------------------

    /**
     * For any string that is null or composed entirely of whitespace characters,
     * passing it as the {@code password} field of a {@link LoginCommand} to
     * {@link CredentialValidator#validate} SHALL throw a {@link ValidationException}
     * with a message containing "password is required".
     *
     * <p>Validates: Requirements 2.2</p>
     * Tag: Feature: user-authentication, Property 5: Blank password is always rejected by CredentialValidator
     */
    @Property(tries = 100)
    @Tag("Feature_user-authentication")
    @Tag("Property_5_Blank_password_is_always_rejected_by_CredentialValidator")
    void blankPassword_alwaysThrowsValidationException(
            @ForAll("validEmail") String email,
            @ForAll("blankOrNull") String blankPassword) {

        LoginCommand command = new LoginCommand(email, blankPassword);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("password is required");
    }
}
