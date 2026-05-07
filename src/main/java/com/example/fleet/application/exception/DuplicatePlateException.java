package com.example.fleet.application.exception;

/**
 * Thrown by {@link com.example.fleet.application.service.VehicleService} when a
 * {@code CreateVehicleCommand} contains a plate that is already registered in the
 * {@code VehicleRepository}.
 * Maps to HTTP 409 in the global exception handler.
 *
 * <p><strong>Note:</strong> {@code GlobalExceptionHandler} must return a generic
 * {@code "Plate already registered"} message in the HTTP response body and log only
 * a generic warning without echoing the plate value.</p>
 */
public class DuplicatePlateException extends RuntimeException {

    public DuplicatePlateException(String plate) {
        super("Plate already registered: " + plate);
    }
}
