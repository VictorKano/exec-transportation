-- Database schema for Executive Transportation Fleet Management System
-- Create User feature

CREATE TABLE IF NOT EXISTS users (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    phone_number    VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- drivers table
-- CNH column is PII (LGPD personal data — government-issued document number).
-- Future iterations may apply column-level encryption or row-level security.
CREATE TABLE IF NOT EXISTS drivers (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    cnh         VARCHAR(20)  NOT NULL,  -- PII: LGPD personal data
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_drivers          PRIMARY KEY (id),
    CONSTRAINT uq_drivers_cnh      UNIQUE (cnh),
    CONSTRAINT fk_drivers_user_id  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- vehicles table
CREATE TABLE IF NOT EXISTS vehicles (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    plate      VARCHAR(20)  NOT NULL,
    brand      VARCHAR(100) NOT NULL,
    model      VARCHAR(100) NOT NULL,
    year       INT          NOT NULL,
    driver_id  UUID,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_vehicles           PRIMARY KEY (id),
    CONSTRAINT uq_vehicles_plate     UNIQUE (plate),
    CONSTRAINT fk_vehicles_driver_id FOREIGN KEY (driver_id) REFERENCES drivers(id)
);
