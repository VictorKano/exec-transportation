package com.example.fleet.infrastructure.web;

import com.example.fleet.application.command.CreateVehicleCommand;
import com.example.fleet.application.response.VehicleResponse;
import com.example.fleet.application.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for vehicle management endpoints.
 * Exposes POST /api/v1/vehicles for vehicle registration.
 *
 * Requires a valid JWT — protected by the existing anyRequest().authenticated() rule
 * in SecurityConfig. No changes to SecurityConfig are needed.
 *
 * Requirements: 6.1, 6.2, 6.3, 6.6
 */
@Tag(name = "Vehicles")
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * Registers a new vehicle in the fleet.
     *
     * @param request the vehicle registration request containing plate, brand, model, year,
     *                and optional driverId
     * @return HTTP 201 with the created vehicle's fields (id, plate, brand, model, year, driverId)
     */
    @Operation(summary = "Register a new vehicle")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Vehicle created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @ApiResponse(responseCode = "404", description = "Driver not found"),
        @ApiResponse(responseCode = "409", description = "Plate already registered")
    })
    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        // Map web DTO to application command
        CreateVehicleCommand command = new CreateVehicleCommand(
                request.plate(),
                request.brand(),
                request.model(),
                request.year(),
                request.driverId()
        );

        // Delegate to application service
        VehicleResponse response = vehicleService.createVehicle(command);

        // Return HTTP 201 Created with the response body
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
