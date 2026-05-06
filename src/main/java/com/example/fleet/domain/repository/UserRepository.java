package com.example.fleet.domain.repository;

import com.example.fleet.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository interface for User persistence.
 * No Spring, JPA, or framework annotations — implementations live in the infrastructure layer.
 */
public interface UserRepository {

    /**
     * Persists the given user and returns the saved instance.
     *
     * @param user the user to save
     * @return the saved user
     */
    User save(User user);

    /**
     * Returns {@code true} if a user with the given email already exists.
     *
     * @param email the email address to check
     * @return {@code true} if the email is already registered, {@code false} otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds a user by their email address.
     *
     * @param email the email address to look up
     * @return an {@link Optional} containing the user if found, or empty if no user has that email
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their unique identifier.
     *
     * @param id the UUID of the user to look up
     * @return an {@link Optional} containing the user if found, or empty if no user has that id
     */
    Optional<User> findById(UUID id);
}
