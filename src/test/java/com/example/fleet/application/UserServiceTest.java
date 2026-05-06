package com.example.fleet.application;

import com.example.fleet.application.command.CreateUserCommand;
import com.example.fleet.application.exception.DuplicateEmailException;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.response.UserResponse;
import com.example.fleet.application.service.UserService;
import com.example.fleet.application.validator.UserValidator;
import com.example.fleet.domain.model.User;
import com.example.fleet.domain.port.PasswordEncoder;
import com.example.fleet.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 * No Spring context — pure JUnit 5 + Mockito.
 * Tests are written before the implementation (TDD).
 * Requirements: 8.1
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserValidator userValidator;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, userValidator);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private CreateUserCommand validCommand() {
        return new CreateUserCommand(
                "Jane Doe",
                "jane.doe@example.com",
                "S3cur3P@ss",
                "+1-555-0100"
        );
    }

    // -----------------------------------------------------------------------
    // Successful creation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("successful user creation")
    class SuccessfulCreation {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
            when(passwordEncoder.encode("S3cur3P@ss")).thenReturn("$2a$10$hashedpassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("returns UserResponse with id, name, email, phoneNumber matching input")
        void createUser_returnsCorrectUserResponse() {
            CreateUserCommand command = validCommand();

            UserResponse response = userService.createUser(command);

            assertThat(response.id()).isNotNull();
            assertThat(response.name()).isEqualTo("Jane Doe");
            assertThat(response.email()).isEqualTo("jane.doe@example.com");
            assertThat(response.phoneNumber()).isEqualTo("+1-555-0100");
        }

        @Test
        @DisplayName("response id is a valid UUID")
        void createUser_responseIdIsUUID() {
            UserResponse response = userService.createUser(validCommand());

            assertThat(response.id()).isNotNull();
        }

        @Test
        @DisplayName("password is never included in the response")
        void createUser_passwordNotInResponse() {
            CreateUserCommand command = validCommand();

            UserResponse response = userService.createUser(command);

            // UserResponse only has id, name, email, phoneNumber — no password field.
            // Verify none of the string fields equal the plain-text password or the hash.
            assertThat(response.name()).isNotEqualTo(command.password());
            assertThat(response.email()).isNotEqualTo(command.password());
            assertThat(response.phoneNumber()).isNotEqualTo(command.password());
        }

        @Test
        @DisplayName("save is called exactly once with a User containing the hashed password")
        void createUser_saveCalledOnceWithHashedPassword() {
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            userService.createUser(validCommand());

            verify(userRepository, times(1)).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getHashedPassword()).isEqualTo("$2a$10$hashedpassword");
            assertThat(savedUser.getHashedPassword()).isNotEqualTo("S3cur3P@ss");
        }

        @Test
        @DisplayName("validator is called before any repository interaction")
        void createUser_validatorCalledFirst() {
            userService.createUser(validCommand());

            verify(userValidator, times(1)).validate(any(CreateUserCommand.class));
        }

        @Test
        @DisplayName("saved User has correct name, email, and phoneNumber")
        void createUser_savedUserHasCorrectFields() {
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            userService.createUser(validCommand());

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getName()).isEqualTo("Jane Doe");
            assertThat(savedUser.getEmail()).isEqualTo("jane.doe@example.com");
            assertThat(savedUser.getPhoneNumber()).isEqualTo("+1-555-0100");
            assertThat(savedUser.getId()).isNotNull();
        }
    }

    // -----------------------------------------------------------------------
    // Duplicate email
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("duplicate email rejection")
    class DuplicateEmailRejection {

        @Test
        @DisplayName("throws DuplicateEmailException when email already exists")
        void createUser_duplicateEmail_throwsDuplicateEmailException() {
            when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(validCommand()))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("jane.doe@example.com");
        }

        @Test
        @DisplayName("save is never called when email is duplicate")
        void createUser_duplicateEmail_saveNeverCalled() {
            when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

            try {
                userService.createUser(validCommand());
            } catch (DuplicateEmailException ignored) {
                // expected
            }

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("DuplicateEmailException message contains the duplicate email address")
        void createUser_duplicateEmail_exceptionMessageContainsEmail() {
            when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(validCommand()))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("jane.doe@example.com");
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
        void createUser_validationFails_throwsValidationException() {
            doThrow(new ValidationException("name is required"))
                    .when(userValidator).validate(any(CreateUserCommand.class));

            assertThatThrownBy(() -> userService.createUser(validCommand()))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("save is never called when validation fails")
        void createUser_validationFails_saveNeverCalled() {
            doThrow(new ValidationException("email format is invalid"))
                    .when(userValidator).validate(any(CreateUserCommand.class));

            try {
                userService.createUser(validCommand());
            } catch (ValidationException ignored) {
                // expected
            }

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("existsByEmail is never called when validation fails")
        void createUser_validationFails_existsByEmailNeverCalled() {
            doThrow(new ValidationException("password must be at least 8 characters"))
                    .when(userValidator).validate(any(CreateUserCommand.class));

            try {
                userService.createUser(validCommand());
            } catch (ValidationException ignored) {
                // expected
            }

            verify(userRepository, never()).existsByEmail(anyString());
        }
    }
}
