package com.example.fleet.infrastructure.web;

import com.example.fleet.application.command.CreateDriverCommand;
import com.example.fleet.application.response.DriverResponse;
import com.example.fleet.application.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for driver management endpoints.
 * Exposes POST /api/v1/drivers for driver registration.
 *
 * Requires a valid JWT — protected by the existing anyRequest().authenticated() rule
 * in SecurityConfig. No changes to SecurityConfig are needed.
 *
 * Requirements: 9.1, 9.2
 */
@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    /**
     * Creates a new driver profile linked to an existing user.
     *
     * @param request the driver registration request containing userId, cnh, and status
     * @return HTTP 201 with the created driver's fields (id, userId, cnh, status)
     */
    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody CreateDriverRequest request) {
        // Map web DTO to application command
        CreateDriverCommand command = new CreateDriverCommand(
                request.userId(),
                request.cnh(),
                request.status()
        );

        // Delegate to application service
        DriverResponse response = driverService.createDriver(command);

        // Return HTTP 201 Created with the response body
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
