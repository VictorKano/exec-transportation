package com.example.fleet.application;

import com.example.fleet.application.command.LoginCommand;
import com.example.fleet.application.exception.InvalidCredentialsException;
import com.example.fleet.application.exception.ValidationException;
import com.example.fleet.application.response.AuthResponse;
import com.example.fleet.application.service.AuthenticationService;
import com.example.fleet.application.validator.CredentialValidator;
import com.example.fleet.domain.model.User;
import com.example.fleet.domain.port.PasswordEncoder;
import com.example.fleet.domain.port.TokenProvider;
import com.example.fleet.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthenticationService}.
 * No Spring context — pure JUnit 5 + Mockito.
 * Tests are written before the implementation (TDD).
 * Requirements: 9.1
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private CredentialValidator credentialValidator;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userRepository, passwordEncoder, tokenProvider, credentialValidator);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String USER_EMAIL = "alice@example.com";
    private static final String RAW_PASSWORD = "S3cur3P@ss";
    private static final String HASHED_PASSWORD = "$2a$10$hashedpassword";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.test.token";

    private User testUser() {
        return new User(USER_ID, "Alice", USER_EMAIL, HASHED_PASSWORD, "+1-555-0200");
    }

    private LoginCommand validCommand() {
        return new LoginCommand(USER_EMAIL, RAW_PASSWORD);
    }

    // -----------------------------------------------------------------------
    // Successful login
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("successful login")
    class SuccessfulLogin {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser()));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            when(tokenProvider.generate(USER_ID, USER_EMAIL)).thenReturn(TOKEN);
        }

        @Test
        @DisplayName("returns AuthResponse with correct token, userId, and email")
        void login_returnsCorrectAuthResponse() {
            AuthResponse response = authenticationService.login(validCommand());

            assertThat(response.token()).isEqualTo(TOKEN);
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.email()).isEqualTo(USER_EMAIL);
        }

        @Test
        @DisplayName("tokenProvider.generate is called exactly once")
        void login_tokenProviderCalledOnce() {
            authenticationService.login(validCommand());

            verify(tokenProvider, times(1)).generate(USER_ID, USER_EMAIL);
        }
    }

    // -----------------------------------------------------------------------
    // Unknown email
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("unknown email rejection")
    class UnknownEmailRejection {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when email is not found")
        void login_unknownEmail_throwsInvalidCredentialsException() {
            assertThatThrownBy(() -> authenticationService.login(validCommand()))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("passwordEncoder.matches is never called when email is unknown")
        void login_unknownEmail_passwordEncoderNeverCalled() {
            try {
                authenticationService.login(validCommand());
            } catch (InvalidCredentialsException ignored) {
                // expected
            }

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("tokenProvider.generate is never called when email is unknown")
        void login_unknownEmail_tokenProviderNeverCalled() {
            try {
                authenticationService.login(validCommand());
            } catch (InvalidCredentialsException ignored) {
                // expected
            }

            verify(tokenProvider, never()).generate(any(UUID.class), anyString());
        }
    }

    // -----------------------------------------------------------------------
    // Wrong password
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("wrong password rejection")
    class WrongPasswordRejection {

        @BeforeEach
        void setUpMocks() {
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser()));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when password does not match")
        void login_wrongPassword_throwsInvalidCredentialsException() {
            assertThatThrownBy(() -> authenticationService.login(validCommand()))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("tokenProvider.generate is never called when password is wrong")
        void login_wrongPassword_tokenProviderNeverCalled() {
            try {
                authenticationService.login(validCommand());
            } catch (InvalidCredentialsException ignored) {
                // expected
            }

            verify(tokenProvider, never()).generate(any(UUID.class), anyString());
        }
    }

    // -----------------------------------------------------------------------
    // Validation failure
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("validation failure propagation")
    class ValidationFailure {

        @Test
        @DisplayName("ValidationException from CredentialValidator is propagated to caller")
        void login_blankEmail_throwsValidationException() {
            doThrow(new ValidationException("email is required"))
                    .when(credentialValidator).validate(any(LoginCommand.class));

            LoginCommand commandWithBlankEmail = new LoginCommand("", RAW_PASSWORD);

            assertThatThrownBy(() -> authenticationService.login(commandWithBlankEmail))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("email is required");
        }

        @Test
        @DisplayName("userRepository.findByEmail is never called when validation fails")
        void login_validationFails_repositoryNeverCalled() {
            doThrow(new ValidationException("email is required"))
                    .when(credentialValidator).validate(any(LoginCommand.class));

            LoginCommand commandWithBlankEmail = new LoginCommand("", RAW_PASSWORD);

            try {
                authenticationService.login(commandWithBlankEmail);
            } catch (ValidationException ignored) {
                // expected
            }

            verify(userRepository, never()).findByEmail(anyString());
        }
    }
}
