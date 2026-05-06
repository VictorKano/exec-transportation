# Scratchboard

## Feature: create-driver

### Status
- [x] Requirements document created: `.kiro/specs/create-driver/requirements.md`
- [ ] Design document
- [ ] Tasks

---

### Domain Modeling Decision: Composition vs Inheritance

**Decision: Composition**

`Driver` holds a `userId` (UUID) reference to an existing `User`. It does NOT extend `User` or embed a `User` object.

**Rationale:**

| Concern | Inheritance | Composition (chosen) |
|---|---|---|
| Coupling | `Driver` tightly coupled to `User` fields | `Driver` only knows the `userId` |
| Persistence | Requires JPA table-per-hierarchy or joined strategy | Simple FK column `user_id` in `drivers` table |
| Domain purity | `User` fields leak into `Driver` | `Driver` is a self-contained aggregate |
| Testability | Must construct a full `User` to test `Driver` | Can test `Driver` with just a UUID |
| Flexibility | Changing `User` breaks `Driver` | `User` changes are isolated |

**Domain model shape:**
```java
public class Driver {
    private final UUID id;
    private final UUID userId;   // reference by ID, not by object
    private final String cnh;
    private final DriverStatus status;
}

public enum DriverStatus {
    ACTIVE, INACTIVE
}
```

---

### Key Design Notes

- `DriverService` resolves the User via `UserRepository.findById(userId)` before persisting.
- CNH uniqueness is enforced at the application layer via `DriverRepository.existsByCnh(cnh)`.
- `DriverValidator` mirrors `UserValidator`: pure Java, no Spring, fully unit-testable.
- Tests follow the same pattern as `UserServiceTest` / `UserServicePropertyTest` (jqwik for PBT).
- New exceptions needed: `UserNotFoundException`, `DuplicateCnhException`.
- New endpoint: `POST /api/v1/drivers` → HTTP 201 on success.
- **CNH is PII under LGPD**: must not be logged in plaintext; not PCI (no payment card data).

---

### Data Classification (LGPD)

| Field | Entity | Classification |
|---|---|---|
| `name`, `email`, `phoneNumber` | User | PII |
| `password` (plaintext) | CreateUserCommand | PII + credential — hash immediately, never log |
| `hashedPassword` | User domain | PII (derived) — never expose in responses |
| `cnh` | Driver | PII — government ID, identity theft risk |
| `userId` (UUID FK) | Driver | PII (indirect) — resolves to a person via join |
| `id` (UUID), `status` | User / Driver | Not PII in isolation |

No fields qualify as LGPD "sensitive personal data" (Article 5(II) — health, biometric, racial, etc.).

---
- Status field: enum `DriverStatus { ACTIVE, INACTIVE }` — not a String, to prevent invalid values.
- CNH format: alphanumeric, 1–20 chars (Brazilian CNH is 11 digits, but keeping it flexible for now).
- User lookup: by UUID (`userId`), not by email — consistent with how the domain references entities.
