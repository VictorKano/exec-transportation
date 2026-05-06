package com.example.fleet.infrastructure.persistence;

import com.example.fleet.domain.model.User;
import com.example.fleet.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Infrastructure adapter implementing the domain UserRepository interface.
 * Maps between domain User objects and JPA UserJpaEntity objects.
 */
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        // Map domain User to JPA entity
        UserJpaEntity entity = new UserJpaEntity(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getHashedPassword(),
                user.getPhoneNumber()
        );

        // Persist via JPA repository
        UserJpaEntity savedEntity = jpaRepository.save(entity);

        // Map JPA entity back to domain User
        return new User(
                savedEntity.getId(),
                savedEntity.getName(),
                savedEntity.getEmail(),
                savedEntity.getHashedPassword(),
                savedEntity.getPhoneNumber()
        );
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(entity -> new User(
                        entity.getId(),
                        entity.getName(),
                        entity.getEmail(),
                        entity.getHashedPassword(),
                        entity.getPhoneNumber()
                ));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(entity -> new User(
                        entity.getId(),
                        entity.getName(),
                        entity.getEmail(),
                        entity.getHashedPassword(),
                        entity.getPhoneNumber()
                ));
    }
}
