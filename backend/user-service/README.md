# User Service

## Purpose
The User Service is a core microservice within the APIShield platform. It is responsible for user identity management, account registration, credential handling, role-based access control (RBAC), and stateless JWT access-token authentication.

## Technology Stack
- **Language:** Java 21 LTS
- **Framework:** Spring Boot 4.1.2
- **Data Persistence:** Spring Data JPA, Hibernate ORM, MySQL Connector/J
- **Database Migrations:** Flyway (`flyway-core`, `flyway-mysql`, `spring-boot-starter-flyway`)
- **Security & Authentication:** Spring Security (stateless session, BCrypt password hashing, custom JWT filter)
- **JWT Library:** `com.auth0:java-jwt:4.4.0` (HMAC-SHA256 signing and claims verification)
- **Validation:** Spring Boot Starter Validation (Jakarta Validation)
- **Testing:** JUnit 5, Mockito, Spring Test / MockMvc, H2 Database (MySQL emulation)
- **Monitoring:** Spring Boot Actuator
- **Productivity:** Project Lombok
- **Build Tool:** Apache Maven (via Maven Wrapper `mvnw`)

## Configuration & Environment Variables

Datasource and security settings are loaded from environment variables (see `.env.example`):

| Variable | Description | Default (Local Dev) |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Full JDBC connection URL | `jdbc:mysql://localhost:3306/apishield_user?useSSL=false...` |
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port (TiDB Cloud: `4000`, MySQL: `3306`) | `3306` |
| `DB_NAME` | Database schema name | `apishield_user` |
| `DB_USERNAME` | Database username | `apishield_user` |
| `DB_PASSWORD` | Database password | *(empty)* |
| `JWT_SECRET` | Secret key for HMAC-SHA256 (must be $\ge 32$ bytes / 256 bits) | *Configured dev key* |
| `JWT_ISSUER` | JWT Issuer claim (`iss`) | `apishield-user-service` |
| `JWT_EXPIRATION_SECONDS` | Access token lifespan in seconds | `3600` (1 hour) |

> [!IMPORTANT]
> **Startup Validation:** If `JWT_SECRET` is missing, blank, or shorter than 32 characters (256 bits), the service will immediately fail startup with a descriptive `IllegalStateException`.

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

---

## API Endpoints

### 1. User Registration
`POST /api/v1/auth/register` (Public)

Creates a new developer account. Self-registration automatically assigns the `DEVELOPER` role. Attempting to self-assign `ADMIN` or `API_OWNER` returns `400 Bad Request`.

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

**Response (`201 Created`):**
```json
{
  "id": "e0b9691c-79f9-4b6a-9351-862d2d9ff6cb",
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada.lovelace@example.com",
  "role": "DEVELOPER",
  "accountStatus": "ACTIVE",
  "emailVerified": false,
  "createdAt": "2026-11-20T01:30:00Z"
}
```

---

### 2. User Login
`POST /api/v1/auth/login` (Public)

Authenticates user credentials and issues a signed JWT access token. Returns generic `401 Unauthorized` for both unknown emails and incorrect passwords to prevent user enumeration. Only accounts with `accountStatus == ACTIVE` are allowed to log in (inactive or suspended accounts return `403 Forbidden`).

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ada.lovelace@example.com",
    "password": "SecureP@ssword123"
  }'
```

**Response (`200 OK`):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "e0b9691c-79f9-4b6a-9351-862d2d9ff6cb",
    "firstName": "Ada",
    "lastName": "Lovelace",
    "email": "ada.lovelace@example.com",
    "role": "DEVELOPER",
    "accountStatus": "ACTIVE",
    "emailVerified": false,
    "createdAt": "2026-11-20T01:30:00Z"
  }
}
```

---

### 3. Current User Profile
`GET /api/v1/auth/me` (Protected)

Requires a valid `Bearer <access_token>` in the `Authorization` header.

```bash
curl -X GET http://localhost:8081/api/v1/auth/me \
  -H "Authorization: Bearer <your_jwt_access_token>"
```

**Response (`200 OK`):**
```json
{
  "id": "e0b9691c-79f9-4b6a-9351-862d2d9ff6cb",
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada.lovelace@example.com",
  "role": "DEVELOPER",
  "accountStatus": "ACTIVE",
  "emailVerified": false,
  "createdAt": "2026-11-20T01:30:00Z"
}
```

---

### 4. Actuator Health
`GET /actuator/health` (Public)

Health check endpoint. Returns `{"status": "UP"}`.

---

## How to Test and Run

### Run Tests
The automated test suite runs against an isolated in-memory H2 database in MySQL emulation mode:
```bash
# Windows
.\mvnw.cmd clean test

# Linux / macOS
./mvnw clean test
```

### Package the Service
```bash
# Windows
.\mvnw.cmd clean package

# Linux / macOS
./mvnw clean package
```

### Run the Service
Ensure your database (local MySQL or TiDB Cloud) is reachable:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

---

## Current Implementation Status
**Step 4: Login and JWT Access Token Authentication**
- `com.auth0:java-jwt:4.4.0` integrated for HMAC-SHA256 signing and validation.
- `JwtService` implemented with startup validation ($\ge 32$ byte secret required).
- `JwtAuthenticationFilter` and `JwtAuthenticationEntryPoint` configured in stateless Spring Security filter chain.
- `POST /api/v1/auth/login` implemented with timing/enumeration-safe 401 handling, account status enforcement (`ACTIVE` only), and password hash protection.
- Protected endpoint `GET /api/v1/auth/me` implemented to verify bearer token authentication.
- Comprehensive test suite covering login success, invalid password, unknown email, inactive/suspended accounts, valid/expired/tampered tokens, and public registration.
