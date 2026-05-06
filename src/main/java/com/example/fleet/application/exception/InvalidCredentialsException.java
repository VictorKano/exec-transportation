package com.example.fleet.application.exception;

/**
 * Thrown by {@code AuthenticationService} when login fails due to an unknown email
 * or a password that does not match the stored hash.
 *
 * <p>The message is intentionally generic — it does not reveal whether the email
 * or the password was wrong — to prevent user enumeration attacks.</p>
 *
 * <p>Maps to HTTP 401 in the global exception handler.</p>
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
