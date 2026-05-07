package com.example.fleet.infrastructure.web;

import com.example.fleet.application.command.LoginCommand;
import com.example.fleet.application.response.AuthResponse;
import com.example.fleet.application.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 * Exposes POST /api/v1/auth/login for user login.
 *
 * Requirements: 1.1, 1.2, 1.3, 1.4, 4.4, 8.3
 */
@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Authenticates a user and returns a signed JWT.
     *
     * @param request the login request containing email and password
     * @return HTTP 200 with an {@link AuthResponse} containing the token, userId, and email
     */
    @Operation(summary = "Authenticate a user and obtain a JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful, JWT token returned"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.email(), request.password());
        AuthResponse response = authenticationService.login(command);
        return ResponseEntity.ok(response);
    }
}
