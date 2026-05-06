package com.example.fleet.application.service;

import com.example.fleet.application.command.LoginCommand;
import com.example.fleet.application.exception.InvalidCredentialsException;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.response.AuthResponse;
import com.example.fleet.application.validator.CredentialValidator;
import com.example.fleet.domain.model.User;
import com.example.fleet.domain.port.PasswordEncoder;
import com.example.fleet.domain.port.TokenProvider;
import com.example.fleet.domain.repository.UserRepository;

/**
 * Application-layer service that orchestrates user authentication (login).
 *
 * <p>Dependencies are injected via constructor using domain interfaces only —
 * no infrastructure types are referenced here, preserving Clean Architecture boundaries.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Validate the command via {@link CredentialValidator} (throws {@link ValidationException} on failure).</li>
 *   <li>Look up the user by email via {@link UserRepository} (throws {@link InvalidCredentialsException} if not found).</li>
 *   <li>Verify the password via {@link PasswordEncoder} (throws {@link InvalidCredentialsException} if it does not match).</li>
 *   <li>Generate a JWT via {@link TokenProvider}.</li>
 *   <li>Return an {@link AuthResponse} containing the token and user identity.</li>
 * </ol>
 *
 * Requirements: 1.3, 3.1, 3.2, 3.4, 4.1, 4.4, 4.5, 8.2, 8.4
 */
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final CredentialValidator credentialValidator;

    public AuthenticationService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 TokenProvider tokenProvider,
                                 CredentialValidator credentialValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.credentialValidator = credentialValidator;
    }

    /**
     * Authenticates a user with the given credentials.
     *
     * @param command the login command containing email and password
     * @return an {@link AuthResponse} with the signed JWT, userId, and email
     * @throws ValidationException          if the email or password is blank
     * @throws InvalidCredentialsException  if the email is not registered or the password does not match
     */
    public AuthResponse login(LoginCommand command) {
        // Step 1: validate input — throws ValidationException on any blank field
        credentialValidator.validate(command);

        // Step 2: look up user by email — throws InvalidCredentialsException if not found
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        // Step 3: verify password — throws InvalidCredentialsException if it does not match
        if (!passwordEncoder.matches(command.password(), user.getHashedPassword())) {
            throw new InvalidCredentialsException();
        }

        // Step 4: generate JWT — plain-text password is never passed to the token provider
        String token = tokenProvider.generate(user.getId(), user.getEmail());

        // Step 5: return public fields only — password (plain or hashed) is never included
        return new AuthResponse(token, user.getId(), user.getEmail());
    }
}
