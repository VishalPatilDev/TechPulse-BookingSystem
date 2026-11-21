# 🗓️ Booking System with Race Condition Protection

A Spring Boot REST API that lets users book time slots concurrently without double-booking, enforced through **pessimistic database locking** and transactional guarantees.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Locking Strategy](#locking-strategy)
- [Transaction Boundaries](#transaction-boundaries)
- [How Race Conditions Are Prevented](#how-race-conditions-are-prevented)
- [Security & Roles](#security--roles)
- [API Reference](#api-reference)
- [Default Users](#default-users)
- [Setup & Run](#setup--run)
- [Running Tests](#running-tests)
- [H2 Console](#h2-console)
- [Swagger UI](#swagger-ui)
- [Assumptions & Design Decisions](#assumptions--design-decisions)

---

## Tech Stack

| Layer        | Technology                        |
|--------------|-----------------------------------|
| Framework    | Spring Boot 3.x                   |
| Security     | Spring Security + JWT             |
| Database     | H2 (in-memory)                    |
| ORM          | Spring Data JPA / Hibernate       |
| Validation   | Jakarta Bean Validation           |
| Docs         | SpringDoc OpenAPI (Swagger UI)    |
| Testing      | JUnit 5 + Mockito + MockMvc       |
| Coverage     | JaCoCo (≥ 80%)                    |
| Build        | Maven                             |
| Config       | `application.yaml`                |

---

## Project Structure

```
src/
├── main/java/com/techpulse/booking_system/
│   ├── config/
│   │   ├── DataInitializer.java      # Seeds default admin & user on startup
│   │   ├── SecurityConfig.java       # JWT filter chain, role-based access
│   │   └── SwaggerConfig.java        # OpenAPI / Swagger setup
│   ├── controller/
│   │   ├── WelcomeController.java    # /welcome — login, register, health
│   │   ├── SlotController.java       # /slots — create & list slots
│   │   └── BookingController.java    # /bookings — book & cancel
│   ├── dto/
│   │   ├── request/                  # AuthRequest, BookingRequest, SlotRequest, UserRequestDto
│   │   └── response/                 # ApiResponse, AuthResponse, BookingResponse, SlotResponse
│   ├── entity/
│   │   ├── Slot.java                 # id, startTime, endTime, slotStatus
│   │   ├── Booking.java              # id, slot, user, bookingStatus, createdAt
│   │   ├── User.java                 # id, username, password, role
│   │   ├── SlotStatus.java           # AVAILABLE | BOOKED
│   │   ├── BookingStatus.java        # ACTIVE | CANCELLED
│   │   └── Role.java                 # ADMIN | USER
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ResourceNotFoundException.java
│   │   ├── SlotNotAvailableException.java
│   │   ├── UnauthorizedException.java
│   │   └── UserAlreadyExistsException.java
│   ├── repository/
│   │   ├── SlotRepository.java       # findByIdWithLock — PESSIMISTIC_WRITE
│   │   ├── BookingRepository.java
│   │   └── UserRepository.java
│   ├── security/
│   │   ├── JwtFilter.java
│   │   └── CustomUserDetailsService.java
│   ├── service/
│   │   ├── BookingService.java
│   │   ├── SlotService.java
│   │   └── UserService.java
│   └── util/JwtUtil.java
└── test/                             # Controller + Service tests (≥ 80% coverage)
```

---

## Locking Strategy

**Pessimistic Locking (`PESSIMISTIC_WRITE`)** is used on the `Slot` entity at the database level.

```java
// SlotRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Slot s WHERE s.id = :id")
Optional<Slot> findByIdWithLock(@Param("id") Long id);
```

When a booking request arrives, Hibernate issues a `SELECT ... FOR UPDATE` SQL statement on the slot row. This acquires an **exclusive row-level lock** in the database, so any concurrent transaction trying to fetch the same slot must wait until the lock is released. This eliminates the race window entirely — only one transaction can check and update the slot status at a time.

**Why pessimistic over optimistic?**
Optimistic locking detects conflicts after the fact via version checks and requires the caller to retry. For a booking system under high concurrency, pessimistic locking provides a strict, no-retry guarantee: exactly one request wins, and all others get a clear "already booked" response.

---

## Transaction Boundaries

| Operation              | Annotation       | What It Covers                                         |
|------------------------|------------------|--------------------------------------------------------|
| `bookSlot()`           | `@Transactional` | Lock slot → check status → update slot → save booking |
| `cancelBooking()`      | `@Transactional` | Verify ownership → cancel booking → free slot          |
| `cancelBookingAdmin()` | `@Transactional` | Cancel any booking → free slot                         |

All database changes within a booking or cancellation happen in a **single atomic transaction**. If anything fails mid-way (e.g., saving the booking record), the entire transaction rolls back — the slot status is never left in an inconsistent state.

---

## How Race Conditions Are Prevented

**Scenario:** Two users (User A and User B) simultaneously send `POST /bookings` for slot ID `5`.

```
Time →

User A: BEGIN TX → SELECT slot FOR UPDATE (acquires lock) ──────────────────────────────► checks AVAILABLE → sets BOOKED → saves booking → COMMIT → lock released
User B:            SELECT slot FOR UPDATE (waits at DB)   ──────────────────────────────►  gets slot (now BOOKED) → throws SlotNotAvailableException → TX rollback
```

1. Both requests hit the `BookingService.bookSlot()` method inside a `@Transactional` block.
2. Both call `slotRepository.findByIdWithLock(slotId)`, which issues `SELECT ... FOR UPDATE`.
3. The database serialises the two requests — only one acquires the lock.
4. The first transaction checks `slotStatus == AVAILABLE`, marks it `BOOKED`, saves both the slot and the booking record, and commits — releasing the lock.
5. The second transaction acquires the lock after the commit and reads `slotStatus == BOOKED`, throwing `SlotNotAvailableException` with HTTP 409.

**Key guarantees:**
- No in-memory `synchronized` blocks — the lock lives in the database and survives application restarts.
- No double-booking is possible regardless of the number of concurrent requests.
- Partial updates cannot occur — the slot status and booking record are always consistent.

---

## Security & Roles

Authentication uses **JWT Bearer tokens**.

| Role    | Permissions                                              |
|---------|----------------------------------------------------------|
| `USER`  | Register, login, view slots, book a slot, cancel own booking |
| `ADMIN` | All USER permissions + create slots, cancel any booking  |

---

## API Reference

**Base URL:** `http://localhost:8080/booking-system`

### Auth — `/welcome`

| Method | Endpoint           | Auth     | Description             |
|--------|--------------------|----------|-------------------------|
| GET    | `/welcome`         | None     | Health check            |
| POST   | `/welcome/register`| None     | Register a new user     |
| POST   | `/welcome/login`   | None     | Login and receive a JWT |

#### Register — `POST /welcome/register`
```json
{
  "username": "john",
  "password": "secret123"
}
```

#### Login — `POST /welcome/login`
```json
{
  "username": "john",
  "password": "secret123"
}
```
**Response:**
```json
{
  "success": true,
  "message": "Login Successful",
  "data": {
    "id": 3,
    "username": "john",
    "token": "<JWT>",
    "userRole": "USER"
  }
}
```

---

### Slots — `/slots`

| Method | Endpoint | Role         | Description              |
|--------|----------|--------------|--------------------------|
| POST   | `/slots` | ADMIN        | Create a new time slot   |
| GET    | `/slots` | USER / ADMIN | List all slots           |

#### Create Slot — `POST /slots`
```json
{
  "startTime": "2025-12-01T09:00:00",
  "endTime":   "2025-12-01T10:00:00"
}
```

---

### Bookings — `/bookings`

| Method | Endpoint                   | Role  | Description                    |
|--------|----------------------------|-------|--------------------------------|
| POST   | `/bookings`                | USER  | Book a slot                    |
| POST   | `/bookings/{id}/cancel`    | USER  | Cancel own booking             |
| POST   | `/bookings/admin/{id}/cancel` | ADMIN | Cancel any booking          |

#### Book a Slot — `POST /bookings`
```json
{
  "slotId": 1
}
```

**Success (201):**
```json
{
  "success": true,
  "message": "Booking created successfully",
  "data": {
    "id": 10,
    "slotId": 1,
    "userId": 3,
    "bookingStatus": "ACTIVE",
    "createdAt": "2025-12-01T09:05:00"
  }
}
```

**Conflict (409) — slot already booked:**
```json
{
  "success": false,
  "message": "Slot 1 is already booked!"
}
```

---

## Default Users

The application seeds two users on startup (password from `application.yaml → default.user.password`):

| Username | Password | Role  |
|----------|----------|-------|
| `admin`  | `12345`  | ADMIN |
| `user1`  | `12345`  | USER  |

---

## Setup & Run

### Prerequisites

- Java 17+
- Maven 3.8+

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/booking-system.git
cd booking-system

# 2. Build the project
./mvnw clean package -DskipTests

# 3. Run the application
./mvnw spring-boot:run
```

The server starts at: `http://localhost:8080/booking-system`

---

## Running Tests

```bash
# Run all tests
./mvnw test

# Run tests with JaCoCo coverage report
./mvnw verify

# View the coverage report
open target/site/jacoco/index.html
```

Test coverage is ≥ 80% across controllers and services.

---

## H2 Console

Access the in-memory database at:

```
http://localhost:8080/booking-system/h2-console
```

| Field    | Value              |
|----------|--------------------|
| JDBC URL | `jdbc:h2:mem:bookingdb` |
| Username | `sa`               |
| Password | *(leave blank)*    |

---

## Swagger UI

Interactive API documentation is available at:

```
http://localhost:8080/booking-system/swagger-ui/index.html
```

Click **Authorize** and paste your JWT token (without the `Bearer ` prefix) to test protected endpoints.

---

## Assumptions & Design Decisions

| Decision | Rationale |
|---|---|
| Pessimistic locking chosen over optimistic | No retry logic needed; the first requester wins and others get a clear rejection immediately |
| H2 in-memory database | Assignment requirement; the locking strategy works identically with PostgreSQL/MySQL in production |
| JWT stateless auth | Simple, scalable; no server-side session storage needed |
| `@Transactional` on service layer | Keeps controllers thin; ensures the lock is held for the full business operation |
| `DataInitializer` seeds default users | Allows immediate testing without a separate registration step |
| `Thread.sleep(3000)` in `bookSlot()` | Intentional delay included to make concurrent booking demos observable; remove for production |
| Global exception handler | All errors return a consistent `ApiResponse` shape with meaningful messages |
| `application.yaml` over `application.properties` | As specified in the assignment requirements |
