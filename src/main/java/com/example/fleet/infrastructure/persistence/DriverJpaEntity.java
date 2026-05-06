package com.example.fleet.infrastructure.persistence;

import com.example.fleet.domain.model.DriverStatus;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA entity mapped to the "drivers" table.
 * Infrastructure layer — contains JPA annotations.
 */
@Entity
@Table(name = "drivers")
public class DriverJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    // PII: LGPD personal data — government-issued document number
    @Column(nullable = false, unique = true)
    private String cnh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus status;

    // Default constructor required by JPA
    protected DriverJpaEntity() {
    }

    public DriverJpaEntity(UUID id, UUID userId, String cnh, DriverStatus status) {
        this.id = id;
        this.userId = userId;
        this.cnh = cnh;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getCnh() {
        return cnh;
    }

    public void setCnh(String cnh) {
        this.cnh = cnh;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }
}
