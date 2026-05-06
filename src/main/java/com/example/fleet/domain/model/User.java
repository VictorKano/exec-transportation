package com.example.fleet.domain.model;

import java.util.UUID;

/**
 * Domain entity representing a registered user.
 * Plain Java class — no Spring, JPA, or any framework annotations.
 */
public class User {

    private final UUID id;
    private final String name;
    private final String email;
    private final String hashedPassword;
    private final String phoneNumber;

    public User(UUID id, String name, String email, String hashedPassword, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.phoneNumber = phoneNumber;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
