package com.example.fleet.infrastructure;

import com.example.fleet.application.exception.InvalidTokenException;
import com.example.fleet.domain.model.Claims;
import com.example.fleet.infrastructure.security.JwtTokenProvider;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for {@link JwtTokenProvider}.
 *
 * <p>Instantiates {@code JwtTokenProvider} directly with a fixed test secret and
 * expiration — no Spring context required.</p>
 *
 * <ul>
 *   <li>Property 1: JWT round-trip preserves claims</li>
 *   <li>Property 2: Invalid JWT is always rejected</li>
 * </ul>
 */
class JwtTokenProviderPropertyTest {

    /**
     * A valid Base64-encoded 256-bit (32-byte) HMAC-SHA256 key for testing.
     * Decoded value: "test-secret-key-for-unit-tests!!" (32 bytes)
     */
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzISE=";

    /** 1 hour in milliseconds — long enough that tokens never expire during a test run. */
    private static final long TEST_EXPIRATION_MS = 3_600_000L;

    private JwtTokenProvider tokenProvider;

    @BeforeProperty
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, TEST_EXPIRATION_MS);
    }

    // -----------------------------------------------------------------------
    // Generators
    // -----------------------------------------------------------------------

    /** Arbitrary that produces random UUIDs. */
    @Provide
    Arbitrary<UUID> anyUserId() {
        return Arbitraries.create(UUID::randomUUID);
    }

    /**
     * Arbitrary that produces non-blank email strings of the form
     * {@code <lowercase-letters>@test.com}.
     */
    @Provide
    Arbitrary<String> anyEmail() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(20)
                .map(s -> s + "@test.com");
    }

    /**
     * Arbitrary that produces strings that are NOT valid JWTs.
     * Filters out strings starting with "eyJ" (Base64url-encoded "{") which are
     * likely real JWT headers.
     */
    @Provide
    Arbitrary<String> invalidToken() {
        return Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(200)
                .filter(s -> !s.startsWith("eyJ"));
    }

    // -----------------------------------------------------------------------
    // Property 1: JWT round-trip preserves claims
    // -----------------------------------------------------------------------

    /**
     * For any {@code userId} and {@code email}, generating a JWT and immediately
     * validating it SHALL return {@link Claims} with the same {@code userId} and
     * {@code email} that were used to generate the token.
     *
     * <p><b>Validates: Requirements 5.5, 4.1</b>
     */
    @Property(tries = 100)
    @Tag("Feature_user-authentication")
    @Tag("Property_1_JWT_round_trip_preserves_claims")
    void jwtRoundTrip_preservesClaims(
            @ForAll("anyUserId") UUID userId,
            @ForAll("anyEmail") String email) {

        // Act: generate a token then validate it
        String token = tokenProvider.generate(userId, email);
        Claims claims = tokenProvider.validate(token);

        // Assert: the round-trip preserves both claims exactly
        assertThat(claims.userId())
                .as("validate(generate(userId, email)).userId() must equal the original userId")
                .isEqualTo(userId);

        assertThat(claims.email())
                .as("validate(generate(userId, email)).email() must equal the original email")
                .isEqualTo(email);
    }

    // -----------------------------------------------------------------------
    // Property 2: Invalid JWT is always rejected
    // -----------------------------------------------------------------------

    /**
     * For any string that is not a valid JWT (i.e., does not start with "eyJ"),
     * calling {@link JwtTokenProvider#validate} SHALL always throw
     * {@link InvalidTokenException}.
     *
     * <p><b>Validates: Requirements 5.2, 5.3, 5.4</b>
     */
    @Property(tries = 100)
    @Tag("Feature_user-authentication")
    @Tag("Property_2_Invalid_JWT_is_always_rejected")
    void invalidToken_alwaysRejected(
            @ForAll("invalidToken") String token) {

        assertThatThrownBy(() -> tokenProvider.validate(token))
                .as("validate('%s') must throw InvalidTokenException", token)
                .isInstanceOf(InvalidTokenException.class);
    }
}
