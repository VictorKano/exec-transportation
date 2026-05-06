package com.example.fleet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository interface for DriverJpaEntity.
 * Infrastructure layer — extends Spring Data JpaRepository.
 */
public interface DriverJpaRepository extends JpaRepository<DriverJpaEntity, UUID> {

    /**
     * Derived query method to check if a driver with the given CNH already exists.
     *
     * <p><strong>PII — LGPD:</strong> The {@code cnh} parameter is a government-issued
     * document number. Do not log this value at any level.
     *
     * @param cnh the CNH number to check
     * @return {@code true} if a driver with the CNH exists, {@code false} otherwise
     */
    boolean existsByCnh(String cnh);
}
