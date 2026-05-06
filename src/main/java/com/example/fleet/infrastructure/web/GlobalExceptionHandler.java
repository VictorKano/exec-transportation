package com.example.fleet.infrastructure.web;

import com.example.fleet.application.exception.DuplicateCnhException;
import com.example.fleet.application.exception.DuplicateEmailException;
import com.example.fleet.application.exception.InvalidCredentialsException;
import com.example.fleet.application.exception.UserNotFoundException;
import com.example.fleet.application.exception.ValidationException;
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
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentialsException(
            InvalidCredentialsException ex) {
        logger.warn("Failed login attempt: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid email or password"));
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * Maps to HTTP 500 with a generic message (no internal details exposed).
     * Logs the full exception with stack trace at ERROR level.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred"));
    }
}
