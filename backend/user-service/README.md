# User Service

## Purpose
The User Service is a core microservice within the APIShield platform. It is responsible for user identity management, account registration, credential handling, role-based access control (RBAC), and authentication.

## Technology Stack
- **Language:** Java 21 LTS
- **Framework:** Spring Boot 4.1.2
- **Data Persistence:** Spring Data JPA, Hibernate ORM, MySQL Connector/J
- **Database Migrations:** Flyway (`flyway-core`, `flyway-mysql`, `spring-boot-starter-flyway`)
- **Security:** Spring Security (BCrypt password encoding, stateless requests)
- **Validation:** Spring Boot Starter Validation (Jakarta Validation)
- **Testing:** JUnit 5, Mockito, Spring Test / MockMvc, H2 Database (MySQL emulation)
- **Monitoring:** Spring Boot Actuator
- **Productivity:** Project Lombok
- **Build Tool:** Apache Maven (via Maven Wrapper `mvnw`)

## Database Configuration
The service connects to MySQL (`apishield_user`) with schema validation (`ddl-auto: validate`) and Flyway database migrations enabled.

Environment variables for datasource configuration:
- `DB_HOST`: Host of MySQL server (default: `localhost`)
- `DB_PORT`: Port of MySQL server (default: `3306`)
- `DB_NAME`: Database name (default: `apishield_user`)
- `DB_USERNAME`: Database username (default: `apishield_user`)
- `DB_PASSWORD`: Database password (default: empty)

### Database Schema (`users` table)
- `id`: VARCHAR(36) UUID primary key
- `first_name`: VARCHAR(100) NOT NULL
- `last_name`: VARCHAR(100) NOT NULL
- `email`: VARCHAR(255) NOT NULL UNIQUE (indexed, normalized to lowercase)
- `password_hash`: VARCHAR(255) NOT NULL (BCrypt encrypted)
- `role`: VARCHAR(30) NOT NULL (enum: `ADMIN`, `API_OWNER`, `DEVELOPER`, indexed)
- `account_status`: VARCHAR(30) NOT NULL (enum: `ACTIVE`, `INACTIVE`, `SUSPENDED`, indexed)
- `email_verified`: BOOLEAN NOT NULL DEFAULT FALSE
- `created_at`: TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at`: TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

## API Endpoints

### 1. User Registration
`POST /api/v1/auth/register`

Creates a new developer account. Public self-registration assigns the `DEVELOPER` role by default.

#### Security & Validation Rules:
- First name and last name: required, 1-100 characters.
- Email: required, valid email format, max 255 characters, case-insensitively normalized.
- Password: required, 8-100 characters, containing at least 1 uppercase letter, 1 lowercase letter, 1 digit, and 1 special character (`@#$%^&+=!._-`).
- Self-registration security constraint: Requesting `ADMIN` or `API_OWNER` is explicitly rejected with `400 Bad Request`. Omitting role or providing `DEVELOPER` assigns `DEVELOPER`.
- Duplicate emails are rejected with `409 Conflict`.
- Sensitive fields (`password`, `passwordHash`) are never exposed in responses or logs.

#### Example Request:
```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Ada",
    "lastName": "Lovelace",
    "email": "ada.lovelace@example.com",
    "password": "SecureP@ssword123"
  }'
```

#### Example Response (`201 Created`):
```json
{
  "id": "e0b9691c-79f9-4b6a-9351-862d2d9ff6cb",
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada.lovelace@example.com",
  "role": "DEVELOPER",
  "accountStatus": "ACTIVE",
  "emailVerified": false,
  "createdAt": "2026-11-19T14:09:12.123Z"
}
```

### 2. Actuator Health
`GET /actuator/health`

Exposed publicly for service health checks. Returns `{"status": "UP"}`.

## How to Test and Run

### Run Unit and Integration Tests
Integration tests run against an isolated in-memory H2 database in MySQL emulation mode:
```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### Package the Service
```bash
# Windows
.\mvnw.cmd clean package

# Linux / macOS
./mvnw clean package
```

### Run the Service
Ensure MySQL is accessible on `DB_HOST:DB_PORT` or run locally:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

The service runs on port **8081**.

## Current Implementation Status
**Step 3: Database and Registration Implementation**
- Flyway database migration `V1__create_users_table.sql` created.
- User JPA Entity, Enums (`Role`, `AccountStatus`), and `UserRepository` implemented.
- `POST /api/v1/auth/register` registration API implemented with BCrypt password hashing, email normalization, and role security enforcement.
- Centralized `GlobalExceptionHandler` with standardized RFC-style error responses.
- Stateless Spring Security filter chain permitting public registration and actuator health.
- Comprehensive unit and integration test suite passing with 100% success rate (16/16 tests passing).
