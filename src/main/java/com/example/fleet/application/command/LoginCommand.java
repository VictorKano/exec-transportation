package com.example.fleet.application.command;

/**
 * Input value object (command) for authenticating a user.
 * Plain Java record — no Spring or JPA annotations.
 */
public record LoginCommand(String email, String password) {}
