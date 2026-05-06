package com.example.fleet.domain.repository;

import com.example.fleet.domain.model.Driver;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository interface for Driver persistence.
 * No Spring, JPA, or framework annotations — implementations live in the infrastructure layer.
 *
 * <p><strong>LGPD notice:</strong> The {@code cnh} (Carteira Nacional de Habilitação) field is
 * personally identifiable information (PII) subject to the Lei Geral de Proteção de Dados
 * (LGPD). It must never appear in application logs, exception messages surfaced to clients,
 * or HTTP response bodies.
 */
public interface DriverRepository {

    /**
     * Persists the given driver and returns the saved instance.
     *
     * @param driver the driver to save
     * @return the saved driver
     */
    Driver save(Driver driver);

    /**
     * Returns {@code true} if a driver with the given CNH already exists.
     *
     * <p><strong>PII — LGPD:</strong> The {@code cnh} parameter is a government-issued
     * document number. Do not log this value at any level.
     *
     * @param cnh the CNH number to check
     * @return {@code true} if the CNH is already registered, {@code false} otherwise
     */
    boolean existsByCnh(String cnh);

    /**
     * Finds a driver by their unique identifier.
     *
     * @param id the driver's UUID
     * @return an {@link Optional} containing the driver if found, or empty if no driver has that id
     */
    Optional<Driver> findById(UUID id);
}
