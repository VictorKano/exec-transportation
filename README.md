# Executive Transportation Fleet Management System

A Spring Boot REST API for managing executive transportation services. Currently implements user registration, user authentication (login), driver registration, and vehicle registration with Clean Architecture, TDD, and property-based testing.

## Tech Stack



## Architecture

The project follows **Clean Architecture** with a strict inward dependency rule:

```
infrastructure  →  application  →  domain
```

- **`domain`** — plain Java classes and interfaces; zero framework dependencies
- **`application`** — business logic (services, validators, commands, responses); depends only on domain
- **`infrastructure`** — Spring Boot, JPA, web layer; implements domain/application interfaces

All PII fields (name, email, phone number, CNH) are handled in accordance with Brazil's **LGPD** — never logged, never echoed in error responses, and excluded from responses where not needed.

## Tech Stack

- **Java 17** + **Spring Boot 3.2.5**
- **PostgreSQL** — primary database
- **Spring Data JPA** — persistence layer
- **Spring Security** — BCrypt password hashing
- **JJWT 0.12.6** — JWT generation and validation (infrastructure layer only)
- **Springdoc OpenAPI 2.5.0** — auto-generated OpenAPI 3.0 spec and Swagger UI (`/swagger-ui/index.html`, `/v3/api-docs`)
- **Jakarta Validation** — request validation
- **jqwik 1.8.5** — property-based testing
- **Testcontainers** — integration tests with real PostgreSQL
- **JaCoCo** — code coverage enforcement (≥ 80% on domain + application packages)

## Project Structure

```
src/
├── main/java/com/example/fleet/
│   ├── domain/
│   │   ├── model/          # User, Driver, DriverStatus, Vehicle, Claims (plain Java entities/value objects)
│   │   ├── repository/     # UserRepository, DriverRepository, VehicleRepository (interfaces)
│   │   ├── port/           # PasswordEncoder, TokenProvider (interfaces)
│   │   └── exception/      # InvalidTokenException
│   ├── application/
│   │   ├── command/        # CreateUserCommand, LoginCommand, CreateDriverCommand, CreateVehicleCommand
│   │   ├── response/       # UserResponse, AuthResponse, DriverResponse, VehicleResponse
│   │   ├── service/        # UserService, AuthenticationService, DriverService, VehicleService
│   │   ├── validator/      # UserValidator, CredentialValidator, DriverValidator, VehicleValidator
│   │   └── exception/      # ValidationException, DuplicateEmailException, InvalidCredentialsException,
│   │                       # InvalidTokenException, UserNotFoundException, DuplicateCnhException,
│   │                       # DuplicatePlateException, DriverNotFoundException
│   └── infrastructure/
│       ├── config/         # Spring bean wiring (ApplicationConfig, SecurityConfig, OpenApiConfig)
│       ├── persistence/    # JPA entities, repositories, adapters (User + Driver + Vehicle)
│       ├── security/       # BCryptPasswordEncoderAdapter
│       └── web/            # UserController, DriverController, VehicleController,
│                           # GlobalExceptionHandler, RequestLoggingFilter
└── test/java/com/example/fleet/
    ├── application/        # UserServiceTest, UserValidatorTest, AuthenticationServiceTest,
    │                       # CredentialValidatorTest, DriverServiceTest, DriverValidatorTest,
    │                       # VehicleServiceTest, VehicleValidatorTest
    │                       # + property tests for all of the above (incl. VehicleServicePropertyTest)
    └── infrastructure/     # UserControllerIntegrationTest, DriverControllerIntegrationTest,
                            # VehicleControllerIntegrationTest (Testcontainers)
```

## API Endpoints

### POST /api/v1/users — Create User

**Request body:**
```json
{
  "name": "Jane Doe",
  "email": "jane.doe@example.com",
  "password": "S3cur3P@ss",
  "phoneNumber": "+1-555-0100"
}
```

**Success — HTTP 201:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Jane Doe",
  "email": "jane.doe@example.com",
  "phoneNumber": "+1-555-0100"
}
```

**Error responses:**

| Scenario | Status | Body |
|---|---|---|
| Missing / blank required field | 400 | `{ "error": "<field> is required" }` |
| Invalid email format | 400 | `{ "error": "email format is invalid" }` |
| Password shorter than 8 chars | 400 | `{ "error": "password must be at least 8 characters" }` |
| Email already registered | 409 | `{ "error": "Email already registered: <email>" }` |
| Unexpected server error | 500 | `{ "error": "An unexpected error occurred" }` |

### POST /api/v1/auth/login — Login

**Request body:**
```json
{
  "email": "jane.doe@example.com",
  "password": "S3cur3P@ss"
}
```

**Success — HTTP 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "jane.doe@example.com"
}
```

**Error responses:**

| Scenario | Status | Body |
|---|---|---|
| Missing / blank email or password | 400 | `{ "error": "<field> is required" }` |
| Unknown email or wrong password | 401 | `{ "error": "Invalid email or password" }` |
| Unexpected server error | 500 | `{ "error": "An unexpected error occurred" }` |

### POST /api/v1/drivers — Register Driver

**Authentication:** Required (Bearer JWT)

**Request body:**
```json
{
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "cnh": "12345678901",
  "status": "ACTIVE"
}
```

**Success — HTTP 201:**
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "cnh": "12345678901",
  "status": "ACTIVE"
}
```

**Error responses:**

| Scenario | Status | Body |
|---|---|---|
| Missing / blank required field | 400 | `{ "error": "<field> is required" }` |
| Non-alphanumeric CNH | 400 | `{ "error": "cnh must be alphanumeric" }` |
| CNH longer than 20 characters | 400 | `{ "error": "cnh must be between 1 and 20 characters" }` |
| Referenced userId does not exist | 404 | `{ "error": "User not found: <userId>" }` |
| CNH already registered | 409 | `{ "error": "CNH already registered" }` (raw CNH not echoed — LGPD) |
| No or invalid JWT | 401 | Spring Security default |
| Unexpected server error | 500 | `{ "error": "An unexpected error occurred" }` |

### POST /api/v1/vehicles — Register Vehicle

**Authentication:** Required (Bearer JWT)

**Request body:**
```json
{
  "plate": "ABC1D23",
  "brand": "Toyota",
  "model": "Corolla",
  "year": 2022,
  "driverId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

`driverId` is optional — omit it to register a vehicle without an assigned driver.

**Success — HTTP 201:**
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "plate": "ABC1D23",
  "brand": "Toyota",
  "model": "Corolla",
  "year": 2022,
  "driverId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

**Error responses:**

| Scenario | Status | Body |
|---|---|---|
| Missing / blank required field | 400 | `{ "error": "<field> is required" }` |
| Year before 1886 | 400 | `{ "error": "year must be 1886 or later" }` |
| Year after current year + 1 | 400 | `{ "error": "year must not exceed current year + 1" }` |
| Plate already registered | 409 | `{ "error": "Plate already registered" }` |
| Referenced driverId does not exist | 404 | `{ "error": "Driver not found: <driverId>" }` |
| No or invalid JWT | 401 | Spring Security default |
| Unexpected server error | 500 | `{ "error": "An unexpected error occurred" }` |

## Prerequisites

- Java 17+
- Maven 3.6+
- PostgreSQL 15+ (or Docker for integration tests)

## Running Locally

**1. Start PostgreSQL:**
```bash
docker run -d \
  --name fleet-db \
  -e POSTGRES_DB=fleet \
  -e POSTGRES_USER=fleet_user \
  -e POSTGRES_PASSWORD=fleet_password \
  -p 5432:5432 \
  postgres:15-alpine
```

**2. Apply the schema:**
```bash
psql -h localhost -U fleet_user -d fleet -f src/main/resources/schema.sql
```

**3. Run the application:**
```bash
JWT_SECRET=<base64-encoded-256-bit-key> mvn spring-boot:run
```

`JWT_SECRET` is required — there is no default value in `application.properties`. You can generate a suitable key with:
```bash
openssl rand -base64 32
```

The API will be available at `http://localhost:8080`.

## Running Tests

**Unit + property-based tests only:**
```bash
mvn test -Dtest="UserServiceTest,UserValidatorTest,UserServicePropertyTest,UserValidatorPropertyTest"
```

**All tests (requires Docker for Testcontainers):**
```bash
mvn test
```

**Full build with coverage check:**
```bash
mvn verify
```

The `verify` goal runs all tests and enforces ≥ 80% line coverage on the `domain` and `application` packages via JaCoCo. The build fails if coverage drops below the threshold.

## Database Schema

```sql
CREATE TABLE users (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    phone_number    VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE drivers (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    cnh        VARCHAR(20) NOT NULL,  -- PII: LGPD personal data
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_drivers         PRIMARY KEY (id),
    CONSTRAINT uq_drivers_cnh     UNIQUE (cnh),
    CONSTRAINT fk_drivers_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE vehicles (
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
```

## Testing Strategy

| Layer | Type | Tool |
|---|---|---|
| `UserValidator` | Unit + property-based | JUnit 5 + jqwik |
| `UserService` | Unit + property-based | JUnit 5 + Mockito + jqwik |
| `CredentialValidator` | Unit + property-based | JUnit 5 + jqwik |
| `AuthenticationService` | Unit + property-based | JUnit 5 + Mockito + jqwik |
| `JwtTokenProvider` | Property-based | JUnit 5 + jqwik |
| `POST /api/v1/users` | Integration | Spring Boot Test + MockMvc + Testcontainers |
| `POST /api/v1/auth/login` | Integration | Spring Boot Test + MockMvc + Testcontainers |
| `DriverValidator` | Unit + property-based | JUnit 5 + jqwik |
| `DriverService` | Unit + property-based | JUnit 5 + Mockito + jqwik |
| `POST /api/v1/drivers` | Integration | Spring Boot Test + MockMvc + Testcontainers |
| OpenAPI / Swagger UI | Integration | Spring Boot Test + MockMvc + Testcontainers |
| `VehicleValidator` | Unit + property-based | JUnit 5 + jqwik |
| `VehicleService` | Unit + property-based | JUnit 5 + Mockito + jqwik |
| `POST /api/v1/vehicles` | Integration | Spring Boot Test + MockMvc + Testcontainers |

**Correctness properties verified (create-user):**
1. Valid registration always returns all required public fields
2. Password is never exposed in the response
3. Blank/null required fields are always rejected
4. Invalid email format is always rejected
5. Passwords shorter than 8 characters are always rejected
6. Duplicate email is always rejected
7. Every created user receives a unique UUID
8. Password is stored only as a BCrypt hash

**Correctness properties verified (user-authentication):**
1. JWT round-trip preserves claims (generate → validate returns same userId and email)
2. Invalid JWT is always rejected with `InvalidTokenException`
3. Password and hash are never exposed in the login response or JWT
4. Blank email is always rejected by `CredentialValidator`
5. Blank password is always rejected by `CredentialValidator`
6. Unknown email always causes authentication failure
7. Wrong password always causes authentication failure

**Correctness properties verified (create-driver):**
1. Blank or null CNH is always rejected by `DriverValidator`
2. Non-alphanumeric CNH is always rejected by `DriverValidator`
3. Out-of-range CNH length (0 or > 20 chars) is always rejected by `DriverValidator`
4. Null userId is always rejected by `DriverValidator`
5. Null status is always rejected by `DriverValidator`
6. Unknown userId always throws `UserNotFoundException`; `save` is never called
7. Duplicate CNH always throws `DuplicateCnhException`; `save` is never called
8. Valid command always produces a `DriverResponse` with matching `userId`, `cnh`, and `status`
9. Two valid commands with different CNHs always produce different UUIDs
10. Validation failure always prevents `UserRepository` and `DriverRepository` calls

**Correctness properties verified (create-vehicle):**
1. Valid command always produces a `VehicleResponse` with all fields matching the command and a non-null `id`
2. Two valid commands with different plates always produce different UUIDs
3. Blank required fields (plate, brand, model) are always rejected with the appropriate `"<field> is required"` message
4. Duplicate plate always throws `DuplicatePlateException`; `save` is never called
5. Unknown `driverId` always throws `DriverNotFoundException`; `save` is never called
6. Year below 1886 is always rejected with `"year must be 1886 or later"`
7. Year above current year + 1 is always rejected with `"year must not exceed current year + 1"`
8. Year in the valid range [1886, currentYear + 1] is always accepted
9. Validation failure always prevents all repository calls (`existsByPlate` and `save`)

## Security Notes

- Passwords are hashed with **BCrypt** before persistence — plain text is never stored or logged
- The `hashedPassword` field is excluded from all API responses
- CNH (driver license number) is treated as PII under Brazil's LGPD — never logged, never echoed in error responses
- Request logging captures HTTP method and path only — request bodies are never logged
- Unexpected errors return a generic message with no internal details exposed
