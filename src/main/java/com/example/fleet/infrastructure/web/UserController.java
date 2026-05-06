package com.example.fleet.infrastructure.web;

import com.example.fleet.application.command.CreateUserCommand;
import com.example.fleet.application.response.UserResponse;
import com.example.fleet.application.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user management endpoints.
 * Exposes POST /api/v1/users for user registration.
 *
 * Requirements: 1.1, 1.2, 1.3, 5.3
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new user account.
     *
     * @param request the user registration request containing name, email, password, and phoneNumber
     * @return HTTP 201 with the created user's public fields (id, name, email, phoneNumber)
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        // Map web DTO to application command
        CreateUserCommand command = new CreateUserCommand(
                request.name(),
                request.email(),
                request.password(),
                request.phoneNumber()
        );

        // Delegate to application service
        UserResponse response = userService.createUser(command);

        // Return HTTP 201 Created with the response body
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
