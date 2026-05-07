package com.example.fleet.application.service;

import com.example.fleet.application.command.CreateVehicleCommand;
import com.example.fleet.application.exception.DriverNotFoundException;
import com.example.fleet.application.exception.DuplicatePlateException;
import com.example.fleet.application.response.VehicleResponse;
import com.example.fleet.application.validator.VehicleValidator;
import com.example.fleet.domain.model.Vehicle;
import com.example.fleet.domain.repository.DriverRepository;
import com.example.fleet.domain.repository.VehicleRepository;

import java.util.UUID;

/**
 * Application-layer service that orchestrates vehicle creation.
 *
 * <p>Dependencies are injected via constructor using domain interfaces only —
 * no infrastructure types are referenced here, preserving Clean Architecture boundaries.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Validate the command via {@link VehicleValidator} (throws {@link com.example.fleet.application.exception.ValidationException}
 *       on failure; no repository calls are made before this step).</li>
 *   <li>Check plate uniqueness via {@link VehicleRepository#existsByPlate} (throws
 *       {@link DuplicatePlateException} if already registered; {@code save} is never called in this case).</li>
 *   <li>If {@code driverId} is non-null, verify the referenced driver exists via
 *       {@link DriverRepository#findById} (throws {@link DriverNotFoundException} if absent;
 *       {@code save} is never called in this case).</li>
 *   <li>Generate a random {@link UUID} as the vehicle's identity in the application layer.</li>
 *   <li>Construct the {@link Vehicle} domain object and persist it via {@link VehicleRepository#save}.</li>
 *   <li>Return a {@link VehicleResponse} with the persisted vehicle's public fields.</li>
 * </ol>
 *
 * Requirements: 1.1, 1.2, 1.3, 2.8, 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 5.4
 */
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final VehicleValidator vehicleValidator;

    public VehicleService(VehicleRepository vehicleRepository,
                          DriverRepository driverRepository,
                          VehicleValidator vehicleValidator) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.vehicleValidator = vehicleValidator;
    }

    /**
     * Creates a new vehicle from the given command.
     *
     * @param command the vehicle creation command containing plate, brand, model, year, and optional driverId
     * @return a {@link VehicleResponse} with the created vehicle's public fields
     * @throws com.example.fleet.application.exception.ValidationException if any field fails validation
     * @throws DuplicatePlateException if the plate is already registered
     * @throws DriverNotFoundException if the referenced driverId does not exist
     */
    public VehicleResponse createVehicle(CreateVehicleCommand command) {
        // Step 1: validate input — throws ValidationException on any violation;
        // no repository calls are made before this point
        vehicleValidator.validate(command);

        // Step 2: check plate uniqueness — throws DuplicatePlateException if already registered;
        // save is never called in this case
        if (vehicleRepository.existsByPlate(command.plate())) {
            throw new DuplicatePlateException(command.plate());
        }

        // Step 3: optional driver existence check — skipped entirely when driverId is null
        if (command.driverId() != null) {
            driverRepository.findById(command.driverId())
                    .orElseThrow(() -> new DriverNotFoundException(command.driverId()));
        }

        // Step 4: generate a unique identity in the application layer
        UUID id = UUID.randomUUID();

        // Step 5: construct and persist the domain entity
        Vehicle vehicle = new Vehicle(id, command.plate(), command.brand(),
                command.model(), command.year(), command.driverId());
        Vehicle saved = vehicleRepository.save(vehicle);

        // Step 6: return public fields only
        return new VehicleResponse(
                saved.getId(),
                saved.getPlate(),
                saved.getBrand(),
                saved.getModel(),
                saved.getYear(),
                saved.getDriverId()
        );
    }
}
