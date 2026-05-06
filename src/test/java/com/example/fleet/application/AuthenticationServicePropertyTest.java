package com.example.fleet.application;

import com.example.fleet.application.command.LoginCommand;
import com.example.fleet.application.exception.InvalidCredentialsException;
import com.example.fleet.application.response.AuthResponse;
import com.example.fleet.application.service.AuthenticationService;
import com.example.fleet.application.validator.CredentialValidator;
import com.example.fleet.domain.model.User;
import com.example.fleet.domain.port.PasswordEncoder;
import com.example.fleet.domain.port.TokenProvider;
import com.example.fleet.domain.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link AuthenticationService}.
 *
 * <p>Uses jqwik to verify universal correctness properties across a wide input space.
 * No Spring context — pure jqwik + Mockito.</p>
 *
 * <ul>
 *   <li>Property 3: Password and hash are never exposed in the login response or JWT</li>
 *   <li>Property 6: Unknown email always causes authentication failure</li>
 *   <li>Property 7: Wrong password always causes authentication failure</li>
 * </ul>
 */
class AuthenticationServicePropertyTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private TokenProvider tokenProvider;
    private CredentialValidator credentialValidator;
    private AuthenticationService authenticationService;

    @BeforeProperty
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        tokenProvider = Mockito.mock(TokenProvider.class);
        credentialValidator = Mockito.mock(CredentialValidator.class);

        authenticationService = new AuthenticationService(
                userRepository, passwordEncoder, tokenProvider, credentialValidator);
    }

    // -----------------------------------------------------------------------
    // Generators
    // -----------------------------------------------------------------------

    /** Arbitrary that produces a valid non-blank email (local@domain.tld). */
    @Provide
    Arbitrary<String> validEmail() {
        Arbitrary<String> local = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
        Arbitrary<String> domain = Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10);
        Arbitrary<String> tld = Arbitraries.of("com", "org", "net", "io");
        return Combinators.combine(local, domain, tld)
                .as((l, d, t) -> l + "@" + d + "." + t);
    }

    /** Arbitrary that produces a valid non-blank password (ascii, 1-30 chars). */
    @Provide
    Arbitrary<String> validPassword() {
        return Arbitraries.strings().ascii().ofMinLength(1).ofMaxLength(30)
                .filter(s -> !s.isBlank());
    }

    /**
     * Arbitrary that produces a BCrypt-format hash string.
     * Real BCrypt hashes always start with "$2a$" or "$2b$" followed by cost and
     * base64-encoded salt+hash data. This format guarantees the hash contains dollar-sign
     * characters that will never appear in a UUID-based token (hex digits + hyphens only),
     * preventing false positives in the containment assertions.
     */
    @Provide
    Arbitrary<String> bcryptHash() {
        Arbitrary<String> saltAndHash = Arbitraries.strings()
                .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789./")
                .ofMinLength(53)
                .ofMaxLength(53);
        return saltAndHash.map(s -> "$2a$10$" + s);
    }

    // -----------------------------------------------------------------------
    // Property 3: Password and hash are never exposed in the login response or JWT
    // -----------------------------------------------------------------------

    /**
     * For any valid login producing an {@link AuthResponse}, neither the plain-text
     * password nor the BCrypt hash of that password SHALL appear in the {@code token}
     * field (JWT claims), the {@code userId} field, or the {@code email} field of
     * the response.
     *
     * <p><b>Validates: Requirements 4.5, 6.2, 6.3</b>
     */
    @Property(tries = 100)
    @Tag("Feature_user-authentication")
    @Tag("Property_3_Password_and_hash_are_never_exposed_in_the_login_response_or_JWT")
    void passwordAndHash_neverExposedInLoginResponse(
            @ForAll("validEmail") String email,
            @ForAll("validPassword") String password,
            @ForAll("bcryptHash") String bcryptHash) {

        // Arrange: a fixed userId for the mocked user
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Test User", email, bcryptHash, "+1-555-0100");

        // Mock: repository returns the user for the given email
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Mock: encoder confirms the password matches (authentication succeeds)
        when(passwordEncoder.matches(password, bcryptHash)).thenReturn(true);

        // Skip samples where the password is a natural substring of the email address
        // (e.g., password="@" is always in any email like "user@domain.com").
        // This avoids false positives: we are testing that the service does not LEAK
        // the password, not that the email field never shares characters with the password.
        Assume.that(!email.contains(password));

        // Mock: token provider returns a JWT-like token using only base64url characters.
        // The token format (header.hexUUID.sig) cannot contain dollar signs, so it can
        // never contain a BCrypt hash (which always starts with "$2a$").
        // We use Assume to skip the rare case where a short password is a substring of
        // the hex UUID portion of the token.
        String hexUuid = UUID.randomUUID().toString().replace("-", "");
        String safeToken = "eyJhbGciOiJIUzI1NiJ9." + hexUuid + ".sig";
        Assume.that(!safeToken.contains(password));
        when(tokenProvider.generate(userId, email)).thenReturn(safeToken);

        // Mock: validator does nothing (no-op) — inputs are already valid
        doNothing().when(credentialValidator).validate(any(LoginCommand.class));

        // Act
        AuthResponse authResponse = authenticationService.login(new LoginCommand(email, password));

        // Assert: plain-text password does not appear in any response field
        assertThat(authResponse.token())
                .as("token must NOT contain the plain-text password")
                .doesNotContain(password);

        assertThat(authResponse.userId().toString())
                .as("userId must NOT contain the plain-text password")
                .doesNotContain(password);

        assertThat(authResponse.email())
                .as("email field must NOT contain the plain-text password")
                .doesNotContain(password);

        // Assert: BCrypt hash does not appear in any response field
        assertThat(authResponse.token())
                .as("token must NOT contain the BCrypt hash")
                .doesNotContain(bcryptHash);

        assertThat(authResponse.userId().toString())
                .as("userId must NOT contain the BCrypt hash")
                .doesNotContain(bcryptHash);

        assertThat(authResponse.email())
                .as("email field must NOT contain the BCrypt hash")
                .doesNotContain(bcryptHash);
    }

    // -----------------------------------------------------------------------
    // Property 6: Unknown email always causes authentication failure
    // -----------------------------------------------------------------------

    /**
     * For any email address that does not correspond to a user record in the
     * {@code UserRepository}, calling {@link AuthenticationService#login} with that
     * email SHALL throw an {@link InvalidCredentialsException}.
     *
     * <p>The mock repository is configured to return {@code Optional.empty()} for every
     * email, simulating a repository that contains no matching user. The password encoder
     * and token provider must never be called in this scenario.</p>
     *
     * <p><b>Validates: Requirements 3.1</b>
     */
    @Property(tries = 100)
    @Tag("Feature_user-authentication")
    @Tag("Property_6_Unknown_email_always_causes_authentication_failure")
    void unknownEmail_alwaysCausesAuthenticationFailure(
            @ForAll("validEmail") String email,
            @ForAll("validPassword") String password) {

        // Arrange: repository returns empty for every email — no user exists
        when(userRepository.findByEmail(any(String.class))).thenReturn(Optional.empty());

        // Mock: validator does nothing (no-op) — inputs are structurally valid
        doNothing().when(credentialValidator).validate(any(LoginCommand.class));

        // Act & Assert: InvalidCredentialsException must always be thrown
        assertThatThrownBy(() -> authenticationService.login(new LoginCommand(email, password)))
                .as("login with unknown email '%s' must throw InvalidCredentialsException", email)
                .isInstanceOf(InvalidCredentialsException.class);

        // Verify: password encoder and token provider are never consulted
        verify(passwordEncoder, never()).matches(any(), any());
        verify(tokenProvider, never()).generate(any(), any());
    }

    // -----------------------------------------------------------------------
    // Property 7: Wrong password always causes authentication failure
    // -----------------------------------------------------------------------

    /**
     * For any registered user and for any password string that does not match the
     * user's stored BCrypt hash, calling {@link AuthenticationService#login} with
     * that user's email and the wrong password SHALL throw an
     * {@link InvalidCredentialsException}, and {@code tokenProvider.generate} must
     * never be called.
     *
     * <p>The mock {@code PasswordEncoder.matches} is configured to return {@code false}
     * for every attempt, simulating a password mismatch regardless of the input.</p>
     *
     * <p><b>Validates: Requirements 3.2</b>
     */
    @Property(tries = 100)
    @Tag("Feature_user-authentication")
    @Tag("Property_7_Wrong_password_always_causes_authentication_failure")
    void wrongPassword_alwaysCausesAuthenticationFailure(
            @ForAll("validEmail") String email,
            @ForAll("validPassword") String wrongPassword,
            @ForAll("bcryptHash") String bcryptHash) {

        // Arrange: a registered user exists in the repository
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Test User", email, bcryptHash, "+1-555-0100");

        // Mock: repository returns the user — the email is recognised
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Mock: encoder always returns false — the password never matches
        when(passwordEncoder.matches(wrongPassword, bcryptHash)).thenReturn(false);

        // Mock: validator does nothing (no-op) — inputs are structurally valid
        doNothing().when(credentialValidator).validate(any(LoginCommand.class));

        // Act & Assert: InvalidCredentialsException must always be thrown
        assertThatThrownBy(() -> authenticationService.login(new LoginCommand(email, wrongPassword)))
                .as("login with wrong password for email '%s' must throw InvalidCredentialsException", email)
                .isInstanceOf(InvalidCredentialsException.class);

        // Verify: token provider is never called when the password is wrong
        verify(tokenProvider, never()).generate(any(), any());
    }
}
