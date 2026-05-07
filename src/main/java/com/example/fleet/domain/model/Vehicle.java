package com.example.fleet.domain.model;

import java.util.UUID;

/**
 * Domain entity representing a fleet vehicle.
 * Plain Java class — no Spring, JPA, or framework annotations.
 */
public class Vehicle {

    private final UUID id;
    private final String plate;
    private final String brand;
    private final String model;
    private final int year;
    private final UUID driverId;   // nullable — null means unassigned

    public Vehicle(UUID id, String plate, String brand, String model, int year, UUID driverId) {
        this.id = id;
        this.plate = plate;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.driverId = driverId;
    }

    public UUID getId()       { return id; }
    public String getPlate()  { return plate; }
    public String getBrand()  { return brand; }
    public String getModel()  { return model; }
    public int getYear()      { return year; }
    public UUID getDriverId() { return driverId; }
}
