package com.example.fleet.infrastructure.web;

import com.example.fleet.application.exception.DriverNotFoundException;
import com.example.fleet.application.exception.DuplicateCnhException;
import com.example.fleet.application.exception.DuplicateEmailException;
import com.example.fleet.application.exception.DuplicatePlateException;
import com.example.fleet.application.exception.InvalidCredentialsException;
import com.example.fleet.application.exception.UserNotFoundException;
import com.example.fleet.application.exception.ValidationException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * Global exception handler that maps application exceptions to HTTP responses.
 * Returns JSON error responses with format: { "error": "<message>" }
 *
 * Requirements: 2.1–2.6, 3.1, 6.1, 6.4, 7.1, 7.4
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Bean Validation failures from @Valid annotations.
     * Maps to HTTP 400 with the first field error message.
     */
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed — the request body contains invalid or missing fields",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(example = "{\"error\": \"Field error message\"}")
        )
    )
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");

        logger.warn("Validation error: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", errorMessage));
    }

    /**
     * Handles application-layer validation failures.
     * Maps to HTTP 400.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(ValidationException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles user not found errors when a referenced userId does not exist.
     * Maps to HTTP 404.
     *
     * Requirements: 9.4
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException ex) {
        logger.warn("User not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles duplicate CNH registration attempts.
     * Maps to HTTP 409 (Conflict).
     *
     * LGPD compliance: the raw CNH value is NOT logged and NOT echoed in the response body.
     * A generic message is returned to avoid exposing PII.
     *
     * Requirements: 9.5, 10.1, 10.4
     */
    @ApiResponse(
        responseCode = "409",
        description = "Conflict — the CNH is already registered",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(example = "{\"error\": \"CNH already registered\"}")
        )
    )
    @ExceptionHandler(DuplicateCnhException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateCnhException(DuplicateCnhException ex) {
        // LGPD: do NOT log the CNH value — log a generic message only
        logger.warn("Duplicate CNH registration attempt");
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "CNH already registered"));
    }

    /**
     * Handles duplicate email registration attempts.
     * Maps to HTTP 409 (Conflict).
     */
    @ApiResponse(
        responseCode = "409",
        description = "Conflict — the email address is already registered",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(example = "{\"error\": \"Email already registered\"}")
        )
    )
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEmailException(
            DuplicateEmailException ex) {
        logger.warn("Duplicate email error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles invalid credentials during login.
     * Maps to HTTP 401 with a generic message that does not reveal whether
     * the email or the password was incorrect (prevents user enumeration).
     * Logs the failed attempt at WARN level — never logs the password.
     *
     * Requirements: 3.3, 7.1, 7.4
     */
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized — the provided credentials are invalid",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(example = "{\"error\": \"Invalid email or password\"}")
        )
    )
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentialsException(
            InvalidCredentialsException ex) {
        logger.warn("Failed login attempt: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid email or password"));
    }

    /**
     * Handles duplicate plate registration attempts.
     * Maps to HTTP 409 (Conflict).
     *
     * The plate value is NOT logged to avoid leaking business identifiers in logs.
     *
     * Requirements: 6.4
     */
    @ExceptionHandler(DuplicatePlateException.class)
    public ResponseEntity<Map<String, String>> handleDuplicatePlateException(DuplicatePlateException ex) {
        logger.warn("Duplicate plate registration attempt");
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Plate already registered"));
    }

    /**
     * Handles driver not found errors when a referenced driverId does not exist.
     * Maps to HTTP 404.
     *
     * Requirements: 6.5
     */
    @ExceptionHandler(DriverNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDriverNotFoundException(DriverNotFoundException ex) {
        logger.warn("Driver not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * Maps to HTTP 500 with a generic message (no internal details exposed).
     * Logs the full exception with stack trace at ERROR level.
     */
    @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error — an unexpected error occurred",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(example = "{\"error\": \"An unexpected error occurred\"}")
        )
    )
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred"));
    }
}
