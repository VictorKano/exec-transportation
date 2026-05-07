package com.example.fleet.application.service;

import com.example.fleet.application.command.CreateVehicleCommand;
import com.example.fleet.application.exception.DriverNotFoundException;
import com.example.fleet.application.exception.DuplicatePlateException;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.response.VehicleResponse;
import com.example.fleet.application.validator.VehicleValidator;
import com.example.fleet.domain.model.Driver;
import com.example.fleet.domain.model.DriverStatus;
import com.example.fleet.domain.model.Vehicle;
import com.example.fleet.domain.repository.DriverRepository;
import com.example.fleet.domain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VehicleService}.
 * No Spring context — pure JUnit 5 + Mockito.
 * Tests are written before the implementation (TDD).
 *
 * <p>Requirements covered: 1.1, 1.2, 1.4, 1.5, 1.6, 1.7, 2.8, 4.1, 4.2, 4.4, 5.1, 5.2, 5.3, 5.4</p>
 */
@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleValidator vehicleValidator;

    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(vehicleRepository, driverRepository, vehicleValidator);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static final UUID EXISTING_DRIVER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final String VALID_PLATE = "ABC1234";
    private static final String VALID_BRAND = "Toyota";
    private static final String VALID_MODEL = "Corolla";
    private static final int VALID_YEAR = 2020;

    private CreateVehicleCommand validCommandWithoutDriver() {
        return new CreateVehicleCommand(VALID_PLATE, VALID_BRAND, VALID_MODEL, VALID_YEAR, null);
    }

    private CreateVehicleCommand validCommandWithDriver() {
        return new CreateVehicleCommand(VALID_PLATE, VALID_BRAND, VALID_MODEL, VALID_YEAR, EXISTING_DRIVER_ID);
    }

    private Driver stubDriver() {
        return new Driver(EXISTING_DRIVER_ID, UUID.randomUUID(), "CNH12345", DriverStatus.ACTIVE);
    }

    // -----------------------------------------------------------------------
    // Successful creation — no driver assignment
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("successful creation with null driverId")
    class SuccessfulCreationWithoutDriver {

        @BeforeEach
        void setUpMocks() {
            when(vehicleRepository.existsByPlate(VALID_PLATE)).thenReturn(false);
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("response has null driverId when command driverId is null")
        void createVehicle_nullDriverId_responseDriverIdIsNull() {
            VehicleResponse response = vehicleService.createVehicle(validCommandWithoutDriver());

            assertThat(response.driverId()).isNull();
        }

        @Test
        @DisplayName("response id is non-null")
        void createVehicle_nullDriverId_responseIdIsNonNull() {
            VehicleResponse response = vehicleService.createVehicle(validCommandWithoutDriver());

            assertThat(response.id()).isNotNull();
        }

        @Test
        @DisplayName("response plate matches command plate")
        void createVehicle_nullDriverId_responsePlateMatchesCommand() {
            VehicleResponse response = vehicleService.createVehicle(validCommandWithoutDriver());

            assertThat(response.plate()).isEqualTo(VALID_PLATE);
        }

        @Test
        @DisplayName("response brand matches command brand")
        void createVehicle_nullDriverId_responseBrandMatchesCommand() {
            VehicleResponse response = vehicleService.createVehicle(validCommandWithoutDriver());

            assertThat(response.brand()).isEqualTo(VALID_BRAND);
        }

        @Test
        @DisplayName("response model matches command model")
        void createVehicle_nullDriverId_responseModelMatchesCommand() {
            VehicleResponse response = vehicleService.createVehicle(validCommandWithoutDriver());

            assertThat(response.model()).isEqualTo(VALID_MODEL);
        }

        @Test
        @DisplayName("response year matches command year")
        void createVehicle_nullDriverId_responseYearMatchesCommand() {
            VehicleResponse response = vehicleService.createVehicle(validCommandWithoutDriver());

            assertThat(response.year()).isEqualTo(VALID_YEAR);
        }

        @Test
        @DisplayName("save is called exactly once")
        void createVehicle_nullDriverId_saveCalledOnce() {
            vehicleService.createVehicle(validCommandWithoutDriver());

            verify(vehicleRepository, times(1)).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("findById is never called when driverId is null")
        void createVehicle_nullDriverId_findByIdNeverCalled() {
            vehicleService.createVehicle(validCommandWithoutDriver());

            verify(driverRepository, never()).findById(any(UUID.class));
        }
    }

    // -----------------------------------------------------------------------
    // Successful creation — with driver assignment
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("successful creation with valid driverId")
    class SuccessfulCreationWithDriver {

        @BeforeEach
        void setUpMocks() {
            when(vehicleRepository.existsByPlate(VALID_PLATE)).thenReturn(false);
            when(driverRepository.findById(EXISTING_DRIVER_ID)).thenReturn(Optional.of(stubDriver()));
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("response driverId matches command driverId")
        void createVehicle_withDriver_responseDriverIdMatchesCommand() {
            VehicleResponse response = vehicleService.createVehicle(validCommandWithDriver());

            assertThat(response.driverId()).isEqualTo(EXISTING_DRIVER_ID);
        }

        @Test
        @DisplayName("findById is called exactly once with the command driverId")
        void createVehicle_withDriver_findByIdCalledOnce() {
            vehicleService.createVehicle(validCommandWithDriver());

            verify(driverRepository, times(1)).findById(EXISTING_DRIVER_ID);
        }

        @Test
        @DisplayName("save is called exactly once")
        void createVehicle_withDriver_saveCalledOnce() {
            vehicleService.createVehicle(validCommandWithDriver());

            verify(vehicleRepository, times(1)).save(any(Vehicle.class));
        }
    }

    // -----------------------------------------------------------------------
    // Validation failure propagation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("validation failure propagation")
    class ValidationFailure {

        @Test
        @DisplayName("ValidationException from validator is propagated to caller")
        void createVehicle_validationFails_throwsValidationException() {
            doThrow(new ValidationException("plate is required"))
                    .when(vehicleValidator).validate(any(CreateVehicleCommand.class));

            assertThatThrownBy(() -> vehicleService.createVehicle(validCommandWithoutDriver()))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("plate is required");
        }

        @Test
        @DisplayName("existsByPlate is never called when validation fails")
        void createVehicle_validationFails_existsByPlateNeverCalled() {
            doThrow(new ValidationException("brand is required"))
                    .when(vehicleValidator).validate(any(CreateVehicleCommand.class));

            try {
                vehicleService.createVehicle(validCommandWithoutDriver());
            } catch (ValidationException ignored) {
                // expected
            }

            verify(vehicleRepository, never()).existsByPlate(anyString());
        }

        @Test
        @DisplayName("save is never called when validation fails")
        void createVehicle_validationFails_saveNeverCalled() {
            doThrow(new ValidationException("model is required"))
                    .when(vehicleValidator).validate(any(CreateVehicleCommand.class));

            try {
                vehicleService.createVehicle(validCommandWithoutDriver());
            } catch (ValidationException ignored) {
                // expected
            }

            verify(vehicleRepository, never()).save(any(Vehicle.class));
        }
    }

    // -----------------------------------------------------------------------
    // Duplicate plate rejection
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("duplicate plate rejection")
    class DuplicatePlateRejection {

        @BeforeEach
        void setUpMocks() {
            when(vehicleRepository.existsByPlate(VALID_PLATE)).thenReturn(true);
        }

        @Test
        @DisplayName("throws DuplicatePlateException when plate already exists")
        void createVehicle_duplicatePlate_throwsDuplicatePlateException() {
            assertThatThrownBy(() -> vehicleService.createVehicle(validCommandWithoutDriver()))
                    .isInstanceOf(DuplicatePlateException.class);
        }

        @Test
        @DisplayName("save is never called when plate is duplicate")
        void createVehicle_duplicatePlate_saveNeverCalled() {
            try {
                vehicleService.createVehicle(validCommandWithoutDriver());
            } catch (DuplicatePlateException ignored) {
                // expected
            }

            verify(vehicleRepository, never()).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("existsByPlate is called before save (InOrder verification)")
        void createVehicle_duplicatePlate_existsByPlateCalledBeforeSave() {
            try {
                vehicleService.createVehicle(validCommandWithoutDriver());
            } catch (DuplicatePlateException ignored) {
                // expected
            }

            InOrder order = inOrder(vehicleRepository);
            order.verify(vehicleRepository, times(1)).existsByPlate(VALID_PLATE);
            order.verify(vehicleRepository, never()).save(any(Vehicle.class));
        }
    }

    // -----------------------------------------------------------------------
    // Driver not found
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("driver not found")
    class DriverNotFound {

        @BeforeEach
        void setUpMocks() {
            when(vehicleRepository.existsByPlate(VALID_PLATE)).thenReturn(false);
            when(driverRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("throws DriverNotFoundException when findById returns empty")
        void createVehicle_driverNotFound_throwsDriverNotFoundException() {
            assertThatThrownBy(() -> vehicleService.createVehicle(validCommandWithDriver()))
                    .isInstanceOf(DriverNotFoundException.class);
        }

        @Test
        @DisplayName("DriverNotFoundException message contains the driverId")
        void createVehicle_driverNotFound_exceptionMessageContainsDriverId() {
            assertThatThrownBy(() -> vehicleService.createVehicle(validCommandWithDriver()))
                    .isInstanceOf(DriverNotFoundException.class)
                    .hasMessageContaining(EXISTING_DRIVER_ID.toString());
        }

        @Test
        @DisplayName("save is never called when driver is not found")
        void createVehicle_driverNotFound_saveNeverCalled() {
            try {
                vehicleService.createVehicle(validCommandWithDriver());
            } catch (DriverNotFoundException ignored) {
                // expected
            }

            verify(vehicleRepository, never()).save(any(Vehicle.class));
        }
    }
}
