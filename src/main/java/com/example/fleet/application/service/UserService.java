package com.example.fleet.application.service;

import com.example.fleet.application.command.CreateUserCommand;
import com.example.fleet.application.exception.DuplicateEmailException;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.response.UserResponse;
import com.example.fleet.application.validator.UserValidator;
import com.example.fleet.domain.model.User;
import com.example.fleet.domain.port.PasswordEncoder;
import com.example.fleet.domain.repository.UserRepository;

import java.util.UUID;

/**
 * Application-layer service that orchestrates user creation.
 *
 * <p>Dependencies are injected via constructor using domain interfaces only —
 * no infrastructure types are referenced here, preserving Clean Architecture boundaries.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Validate the command via {@link UserValidator} (throws {@link ValidationException} on failure).</li>
 *   <li>Check for duplicate email via {@link UserRepository} (throws {@link DuplicateEmailException} if found).</li>
 *   <li>Generate a random {@link UUID} as the user's identity.</li>
 *   <li>Hash the plain-text password via {@link PasswordEncoder}.</li>
 *   <li>Construct the {@link User} domain object and persist it.</li>
 *   <li>Return a {@link UserResponse} — never including the password.</li>
 * </ol>
 *
 * Requirements: 1.3, 3.1, 4.1, 4.2, 4.3, 5.1, 5.2
 */
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UserValidator userValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userValidator = userValidator;
    }

    /**
     * Creates a new user from the given command.
     *
     * @param command the user creation command containing name, email, password, and phoneNumber
     * @return a {@link UserResponse} with the created user's public fields (never includes password)
     * @throws ValidationException     if any field fails validation
     * @throws DuplicateEmailException if the email is already registered
     */
    public UserResponse createUser(CreateUserCommand command) {
        // Step 1: validate input — throws ValidationException on any violation
        userValidator.validate(command);

        // Step 2: check for duplicate email — throws DuplicateEmailException if already registered
        if (userRepository.existsByEmail(command.email())) {
            throw new DuplicateEmailException(command.email());
        }

        // Step 3: generate a unique identity in the application layer
        UUID id = UUID.randomUUID();

        // Step 4: hash the password — plain-text is discarded after this point
        String hashedPassword = passwordEncoder.encode(command.password());

        // Step 5: construct and persist the domain entity
        User user = new User(id, command.name(), command.email(), hashedPassword, command.phoneNumber());
        userRepository.save(user);

        // Step 6: return public fields only — password (plain or hashed) is never included
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhoneNumber());
    }
}
