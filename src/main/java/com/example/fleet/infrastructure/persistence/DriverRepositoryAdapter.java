package com.example.fleet.infrastructure.persistence;

import com.example.fleet.domain.model.Driver;
import com.example.fleet.domain.repository.DriverRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Infrastructure adapter implementing the domain DriverRepository interface.
 * Maps between domain Driver objects and JPA DriverJpaEntity objects.
 */
@Repository
public class DriverRepositoryAdapter implements DriverRepository {

    private final DriverJpaRepository jpaRepository;

    public DriverRepositoryAdapter(DriverJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Driver save(Driver driver) {
        // Map domain Driver to JPA entity
        DriverJpaEntity entity = new DriverJpaEntity(
                driver.getId(),
                driver.getUserId(),
                driver.getCnh(),
                driver.getStatus()
        );

        // Persist via JPA repository
        DriverJpaEntity savedEntity = jpaRepository.save(entity);

        // Map JPA entity back to domain Driver
        return new Driver(
                savedEntity.getId(),
                savedEntity.getUserId(),
                savedEntity.getCnh(),
                savedEntity.getStatus()
        );
    }

    @Override
    public boolean existsByCnh(String cnh) {
        return jpaRepository.existsByCnh(cnh);
    }

    @Override
    public Optional<Driver> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(entity -> new Driver(
                        entity.getId(),
                        entity.getUserId(),
                        entity.getCnh(),
                        entity.getStatus()
                ));
    }
}
