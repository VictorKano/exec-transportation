package com.example.fleet.domain.model;

import java.util.UUID;

/**
 * Domain entity representing a driver profile.
 * Uses composition: holds a {@code userId} (UUID) reference to an existing {@link User}
 * rather than embedding the User object. This keeps the domain decoupled and simplifies
 * persistence to a single FK column.
 * <p>
 * Plain Java class — no Spring, JPA, or framework annotations.
 */
public class Driver {

    private final UUID id;
    private final UUID userId;
    private final String cnh;
    private final DriverStatus status;

    public Driver(UUID id, UUID userId, String cnh, DriverStatus status) {
        this.id = id;
        this.userId = userId;
        this.cnh = cnh;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCnh() {
        return cnh;
    }

    public DriverStatus getStatus() {
        return status;
    }
}
