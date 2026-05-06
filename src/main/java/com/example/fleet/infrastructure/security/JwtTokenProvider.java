package com.example.fleet.infrastructure.security;

import com.example.fleet.application.exception.InvalidTokenException;
import com.example.fleet.domain.model.Claims;
import com.example.fleet.domain.port.TokenProvider;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Infrastructure implementation of {@link TokenProvider} using JJWT.
 *
 * <p>Generates and validates HMAC-SHA256 signed JWTs. The secret is supplied as a
 * Base64-encoded string (at least 256 bits / 32 bytes) via the {@code jwt.secret}
 * application property.</p>
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * Creates a {@code JwtTokenProvider}.
     *
     * @param base64Secret  Base64-encoded HMAC-SHA256 secret (≥ 256 bits)
     * @param expirationMs  token lifetime in milliseconds (default 24 h)
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {

        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sets the JWT {@code sub} claim to {@code userId.toString()} and adds a
     * custom {@code email} claim.</p>
     */
    @Override
    public String generate(UUID userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidTokenException if the token is expired, has an invalid
     *                               signature, or is malformed
     */
    @Override
    public Claims validate(String token) {
        try {
            io.jsonwebtoken.Claims jwtClaims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(jwtClaims.getSubject());
            String email = jwtClaims.get("email", String.class);

            return new Claims(userId, email);

        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired JWT token", e);
        }
    }
}
