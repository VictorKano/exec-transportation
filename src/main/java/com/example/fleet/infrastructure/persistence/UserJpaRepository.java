package com.example.fleet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository interface for UserJpaEntity.
 * Infrastructure layer — extends Spring Data JpaRepository.
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    /**
     * Derived query method to check if a user with the given email exists.
     *
     * @param email the email address to check
     * @return {@code true} if a user with the email exists, {@code false} otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Derived query method to find a user by their email address.
     *
     * @param email the email address to look up
     * @return an {@link Optional} containing the entity if found, or empty otherwise
     */
    Optional<UserJpaEntity> findByEmail(String email);
}
