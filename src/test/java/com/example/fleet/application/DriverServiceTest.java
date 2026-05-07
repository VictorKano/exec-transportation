package com.example.fleet.application;

import com.example.fleet.application.command.CreateDriverCommand;
import com.example.fleet.application.exception.DuplicateCnhException;
import com.example.fleet.application.exception.UserNotFoundException;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.response.DriverResponse;
import com.example.fleet.application.service.DriverService;
import com.example.fleet.application.validator.DriverValidator;
import com.example.fleet.domain.model.Driver;
import com.example.fleet.domain.model.DriverStatus;
import com.example.fleet.domain.model.User;
import com.example.fleet.domain.repository.DriverRepository;
import com.example.fleet.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DriverService}.
 * No Spring context — pure JUnit 5 + Mockito.
 * Tests are written before the implementation (TDD).
 *
 * <p>Requirements covered: 1.1, 1.2, 1.3, 3.1, 3.2, 3.3, 5.2, 5.3, 6.1, 6.2, 6.3, 8.3, 8.4</p>
 */
@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverValidator driverValidator;

    private DriverService driverService;

    @BeforeEach
    void setUp() {
        driverService = new DriverService(userRepository, driverRepository, driverValidator);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static final UUID EXISTING_USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String VALID_CNH = "ABC12345";

    private CreateDriverCommand validCommand() {
        return new CreateDriverCommand(EXISTING_USER_ID, VALID_CNH, DriverStatus.ACTIVE);
    }

    private User stubUser() {
        return new User(EXISTING_USER_ID, "Jane Doe", "jane@example.com", "hashed", "+1-555-0100");
    }

    private Driver stubSavedDriver(UUID id, CreateDriverCommand command) {
        return new Driver(id, command.userId(), command.cnh(), command.status());
    }

    // -----------------------------------------------------------------------
    // Successful creation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("successful driver creation")
    class SuccessfulCreation {

        private UUID capturedId;

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(stubUser()));
            when(driverRepository.existsByCnh(VALID_CNH)).thenReturn(false);
            when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("returns DriverResponse with non-null id")
        void createDriver_returnsResponseWithNonNullId() {
            DriverResponse response = driverService.createDriver(validCommand());

            assertThat(response.id()).isNotNull();
        }

        @Test
        @DisplayName("response userId matches command userId")
        void createDriver_responseUserIdMatchesCommand() {
            DriverResponse response = driverService.createDriver(validCommand());

            assertThat(response.userId()).isEqualTo(EXISTING_USER_ID);
        }

        @Test
        @DisplayName("response cnh matches command cnh")
        void createDriver_responseCnhMatchesCommand() {
            DriverResponse response = driverService.createDriver(validCommand());

            assertThat(response.cnh()).isEqualTo(VALID_CNH);
        }

        @Test
        @DisplayName("response status matches command status")
        void createDriver_responseStatusMatchesCommand() {
            DriverResponse response = driverService.createDriver(validCommand());

            assertThat(response.status()).isEqualTo(DriverStatus.ACTIVE);
        }

        @Test
        @DisplayName("validator is called exactly once with the command")
        void createDriver_validatorCalledOnce() {
            CreateDriverCommand command = validCommand();

            driverService.createDriver(command);

            verify(driverValidator, times(1)).validate(command);
        }

        @Test
        @DisplayName("findById is called exactly once with the command userId")
        void createDriver_findByIdCalledOnce() {
            driverService.createDriver(validCommand());

            verify(userRepository, times(1)).findById(EXISTING_USER_ID);
        }

        @Test
        @DisplayName("existsByCnh is called exactly once with the command cnh")
        void createDriver_existsByCnhCalledOnce() {
            driverService.createDriver(validCommand());

            verify(driverRepository, times(1)).existsByCnh(VALID_CNH);
        }

        @Test
        @DisplayName("save is called exactly once with a Driver containing correct fields")
        void createDriver_saveCalledOnceWithCorrectDriver() {
            ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);

            driverService.createDriver(validCommand());

            verify(driverRepository, times(1)).save(driverCaptor.capture());
            Driver saved = driverCaptor.getValue();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getUserId()).isEqualTo(EXISTING_USER_ID);
            assertThat(saved.getCnh()).isEqualTo(VALID_CNH);
            assertThat(saved.getStatus()).isEqualTo(DriverStatus.ACTIVE);
        }

        @Test
        @DisplayName("INACTIVE status is preserved in the response")
        void createDriver_inactiveStatusPreserved() {
            CreateDriverCommand command = new CreateDriverCommand(EXISTING_USER_ID, VALID_CNH, DriverStatus.INACTIVE);

            DriverResponse response = driverService.createDriver(command);

            assertThat(response.status()).isEqualTo(DriverStatus.INACTIVE);
        }
    }

    // -----------------------------------------------------------------------
    // User not found
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("user not found")
    class UserNotFound {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("throws UserNotFoundException when findById returns empty")
        void createDriver_userNotFound_throwsUserNotFoundException() {
            assertThatThrownBy(() -> driverService.createDriver(validCommand()))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("UserNotFoundException message contains the userId")
        void createDriver_userNotFound_exceptionMessageContainsUserId() {
            assertThatThrownBy(() -> driverService.createDriver(validCommand()))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(EXISTING_USER_ID.toString());
        }

        @Test
        @DisplayName("save is never called when user is not found")
        void createDriver_userNotFound_saveNeverCalled() {
            try {
                driverService.createDriver(validCommand());
            } catch (UserNotFoundException ignored) {
                // expected
            }

            verify(driverRepository, never()).save(any(Driver.class));
        }
    }

    // -----------------------------------------------------------------------
    // Duplicate CNH
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("duplicate CNH rejection")
    class DuplicateCnhRejection {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(stubUser()));
            when(driverRepository.existsByCnh(VALID_CNH)).thenReturn(true);
        }

        @Test
        @DisplayName("throws DuplicateCnhException when CNH already exists")
        void createDriver_duplicateCnh_throwsDuplicateCnhException() {
            assertThatThrownBy(() -> driverService.createDriver(validCommand()))
                    .isInstanceOf(DuplicateCnhException.class);
        }

        @Test
        @DisplayName("save is never called when CNH is duplicate")
        void createDriver_duplicateCnh_saveNeverCalled() {
            try {
                driverService.createDriver(validCommand());
            } catch (DuplicateCnhException ignored) {
                // expected
            }

            verify(driverRepository, never()).save(any(Driver.class));
        }

        @Test
        @DisplayName("existsByCnh is called before save is attempted")
        void createDriver_duplicateCnh_existsByCnhCalledBeforeSave() {
            try {
                driverService.createDriver(validCommand());
            } catch (DuplicateCnhException ignored) {
                // expected
            }

            verify(driverRepository, times(1)).existsByCnh(VALID_CNH);
            verify(driverRepository, never()).save(any(Driver.class));
        }
    }

    // -----------------------------------------------------------------------
    // Validation failure
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("validation failure propagation")
    class ValidationFailure {

        @Test
        @DisplayName("ValidationException from validator is propagated to caller")
        void createDriver_validationFails_throwsValidationException() {
            doThrow(new ValidationException("userId is required"))
                    .when(driverValidator).validate(any(CreateDriverCommand.class));

            assertThatThrownBy(() -> driverService.createDriver(validCommand()))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("userId is required");
        }

        @Test
        @DisplayName("findById is never called when validation fails")
        void createDriver_validationFails_findByIdNeverCalled() {
            doThrow(new ValidationException("cnh is required"))
                    .when(driverValidator).validate(any(CreateDriverCommand.class));

            try {
                driverService.createDriver(validCommand());
            } catch (ValidationException ignored) {
                // expected
            }

            verify(userRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("save is never called when validation fails")
        void createDriver_validationFails_saveNeverCalled() {
            doThrow(new ValidationException("status is required"))
                    .when(driverValidator).validate(any(CreateDriverCommand.class));

            try {
                driverService.createDriver(validCommand());
            } catch (ValidationException ignored) {
                // expected
            }

            verify(driverRepository, never()).save(any(Driver.class));
        }

        @Test
        @DisplayName("existsByCnh is never called when validation fails")
        void createDriver_validationFails_existsByCnhNeverCalled() {
            doThrow(new ValidationException("cnh must be alphanumeric"))
                    .when(driverValidator).validate(any(CreateDriverCommand.class));

            try {
                driverService.createDriver(validCommand());
            } catch (ValidationException ignored) {
                // expected
            }

            verify(driverRepository, never()).existsByCnh(anyString());
        }
    }
}
