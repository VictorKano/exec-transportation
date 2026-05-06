package com.example.fleet.application.command;

/**
 * Input value object (command) for creating a new user.
 * Plain Java record — no Spring or JPA annotations.
 */
public record CreateUserCommand(
        String name,
        String email,
        String password,
        String phoneNumber
) {}
