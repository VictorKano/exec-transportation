package com.example.fleet.application.service;

import com.example.fleet.application.command.CreateDriverCommand;
import com.example.fleet.application.exception.DuplicateCnhException;
import com.example.fleet.application.exception.UserNotFoundException;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.response.DriverResponse;
import com.example.fleet.application.validator.DriverValidator;
import com.example.fleet.domain.model.Driver;
import com.example.fleet.domain.repository.DriverRepository;
import com.example.fleet.domain.repository.UserRepository;

import java.util.UUID;

/**
 * Application-layer service that orchestrates driver creation.
 *
 * <p>Dependencies are injected via constructor using domain interfaces only —
 * no infrastructure types are referenced here, preserving Clean Architecture boundaries.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Validate the command via {@link DriverValidator} (throws {@link ValidationException} on failure;
 *       no repository calls are made before this step).</li>
 *   <li>Verify the referenced user exists via {@link UserRepository#findById} (throws
 *       {@link UserNotFoundException} if absent).</li>
 *   <li>Check CNH uniqueness via {@link DriverRepository#existsByCnh} (throws
 *       {@link DuplicateCnhException} if already registered; {@code save} is never called in this case).</li>
 *   <li>Generate a random {@link UUID} as the driver's identity in the application layer.</li>
 *   <li>Construct the {@link Driver} domain object and persist it via {@link DriverRepository#save}.</li>
 *   <li>Return a {@link DriverResponse} with the persisted driver's public fields.</li>
 * </ol>
 *
 * Requirements: 1.1, 1.2, 1.3, 3.1, 3.2, 3.3, 5.2, 5.3, 6.1, 6.2, 6.3, 6.4, 8.3, 8.4
 */
public class DriverService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final DriverValidator driverValidator;

    public DriverService(UserRepository userRepository,
                         DriverRepository driverRepository,
                         DriverValidator driverValidator) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.driverValidator = driverValidator;
    }

    /**
     * Creates a new driver profile from the given command.
     *
     * @param command the driver creation command containing userId, cnh, and status
     * @return a {@link DriverResponse} with the created driver's public fields
     * @throws ValidationException     if any field fails validation
     * @throws UserNotFoundException   if no user with the given userId exists
     * @throws DuplicateCnhException   if the CNH is already registered
     */
    public DriverResponse createDriver(CreateDriverCommand command) {
        // Step 1: validate input — throws ValidationException on any violation;
        // no repository calls are made before this point
        driverValidator.validate(command);

        // Step 2: verify the referenced user exists — throws UserNotFoundException if absent
        userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        // Step 3: check CNH uniqueness — throws DuplicateCnhException if already registered;
        // save is never called in this case
        if (driverRepository.existsByCnh(command.cnh())) {
            throw new DuplicateCnhException(command.cnh());
        }

        // Step 4: generate a unique identity in the application layer
        UUID id = UUID.randomUUID();

        // Step 5: construct and persist the domain entity
        Driver driver = new Driver(id, command.userId(), command.cnh(), command.status());
        Driver saved = driverRepository.save(driver);

        // Step 6: return public fields only
        return new DriverResponse(saved.getId(), saved.getUserId(), saved.getCnh(), saved.getStatus());
    }
}
