# User Service

## Purpose
The User Service is a core microservice within the APIShield platform. It is responsible for user identity management, account registration, credential handling, role-based access control (RBAC), and stateless JWT access-token authentication with refresh-token rotation and session lifecycle management.

## Technology Stack
- **Language:** Java 21 LTS
- **Framework:** Spring Boot 4.1.2
- **Data Persistence:** Spring Data JPA, Hibernate ORM, MySQL Connector/J
- **Database Migrations:** Flyway (`flyway-core`, `flyway-mysql`, `spring-boot-starter-flyway`)
- **Security & Authentication:** Spring Security (stateless session, BCrypt password hashing, custom JWT filter, HttpOnly cookies)
- **JWT Library:** `com.auth0:java-jwt:4.4.0` (HMAC-SHA256 signing and claims verification)
- **Validation:** Spring Boot Starter Validation (Jakarta Validation)
- **Testing:** JUnit 5, Mockito, Spring Test / MockMvc, H2 Database (MySQL emulation)
- **Monitoring:** Spring Boot Actuator
- **Productivity:** Project Lombok
- **Build Tool:** Apache Maven (via Maven Wrapper `mvnw`)

## Configuration & Environment Variables

Datasource, JWT, and Refresh Token settings are loaded from environment variables (see `.env.example`):

| Variable | Description | Default (Local Dev) |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Full JDBC connection URL | `jdbc:mysql://localhost:3306/apishield_user?useSSL=false...` |
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port (TiDB Cloud: `4000`, MySQL: `3306`) | `3306` |
| `DB_NAME` | Database schema name | `apishield_user` |
| `DB_USERNAME` | Database username | `apishield_user` |
| `DB_PASSWORD` | Database password | *(empty)* |
| `JWT_SECRET` | Secret key for HMAC-SHA256 ($\ge 32$ bytes / 256 bits) | *Configured dev key* |
| `JWT_ISSUER` | JWT Issuer claim (`iss`) | `apishield-user-service` |
| `JWT_EXPIRATION_SECONDS` | Access token lifespan in seconds | `3600` (1 hour) |
| `JWT_REFRESH_EXPIRATION_SECONDS` | Refresh token lifespan in seconds | `604800` (7 days) |
| `JWT_REFRESH_COOKIE_NAME` | Name of the refresh cookie | `refresh_token` |
| `JWT_REFRESH_COOKIE_SECURE` | Enforce HTTPS-only cookie transmission | `false` (set `true` in production) |
| `JWT_REFRESH_COOKIE_SAME_SITE` | Cookie SameSite policy | `Lax` (`Strict` / `Lax`) |
| `JWT_REFRESH_COOKIE_PATH` | Path restriction for cookie | `/api/v1/auth` |

> [!IMPORTANT]
> **Startup Validation:** If `JWT_SECRET` is missing, blank, or shorter than 32 characters (256 bits), the service will immediately fail startup with a descriptive `IllegalStateException`.

---

## Database Schema

### 1. `users` Table (`V1__create_users_table.sql`)
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

### 2. `refresh_tokens` Table (`V2__create_refresh_tokens_table.sql`)
- `id`: VARCHAR(36) UUID primary key
- `user_id`: VARCHAR(36) NOT NULL (foreign key referencing `users.id` with CASCADE delete)
- `token_hash`: VARCHAR(64) NOT NULL UNIQUE (SHA-256 hash of opaque token, never plaintext)
- `family_id`: VARCHAR(36) NOT NULL (UUID grouping token rotations for reuse detection)
- `created_at`: TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `expires_at`: TIMESTAMP NOT NULL
- `revoked_at`: TIMESTAMP NULL
- `replaced_by_token_id`: VARCHAR(36) NULL (points to successor token)
- `revoke_reason`: VARCHAR(100) NULL (`ROTATED`, `LOGOUT`, `LOGOUT_ALL`, `REUSE_DETECTED`, `EXPIRED`)

### 3. `password_reset_tokens` Table (`V3__create_password_reset_tokens_table.sql`)
- `id`: VARCHAR(36) UUID primary key
- `user_id`: VARCHAR(36) NOT NULL (foreign key referencing `users.id` with CASCADE delete)
- `token_hash`: VARCHAR(64) NOT NULL UNIQUE (SHA-256 hash of single-use reset token)
- `expires_at`: TIMESTAMP NOT NULL
- `used_at`: TIMESTAMP NULL
- `created_at`: TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

### 4. `email_verification_tokens` Table (`V4__create_email_verification_tokens_table.sql`)
- `id`: VARCHAR(36) UUID primary key
- `user_id`: VARCHAR(36) NOT NULL (foreign key referencing `users.id` with CASCADE delete)
- `token_hash`: VARCHAR(64) NOT NULL UNIQUE (SHA-256 hash of verification token)
- `expires_at`: TIMESTAMP NOT NULL
- `used_at`: TIMESTAMP NULL
- `created_at`: TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- *Also alters `users` to add audit columns: `suspension_reason VARCHAR(255)`, `suspended_at TIMESTAMP`, `suspended_by VARCHAR(36)`.*

### 5. `audit_logs` Table (`V5__create_audit_logs_table.sql`)
- `id`: VARCHAR(36) UUID primary key
- `actor_id`: VARCHAR(36) NOT NULL (UUID of administrator)
- `target_id`: VARCHAR(36) NOT NULL (UUID of target user)
- `action`: VARCHAR(50) NOT NULL (`ROLE_CHANGE`, `SUSPEND_USER`, `REACTIVATE_USER`, `ACTIVATE_USER`, `DEACTIVATE_USER`, `DELETE_USER`, `UPDATE_DETAILS`)
- `reason`: VARCHAR(255) NULL
- `created_at`: TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

---

## Refresh-Token Lifecycle & Security Design

1. **Opaque Cryptographic Tokens:** Generated using `SecureRandom` with 256 bits (32 bytes) of cryptographic entropy, encoded in Base64URL.
2. **One-Way Hashing:** Only the SHA-256 hex digest (64 characters) is stored in the database. Plaintext tokens are never stored, logged, or serialized into JSON responses.
3. **HttpOnly Cookie Delivery:** Refresh tokens are delivered exclusively via `HttpOnly`, `SameSite=Lax`, `Path=/api/v1/auth` cookies to prevent XSS exfiltration.
4. **Strict Token Rotation:** Every `POST /api/v1/auth/refresh` consumes the presented refresh token, marks it revoked with reason `ROTATED`, and issues a new refresh token under the same `family_id`.
5. **Automatic Reuse Detection:** If an already-rotated or revoked token is presented again (indicating token theft or replay), the entire token family is immediately revoked with reason `REUSE_DETECTED`, the client cookie is cleared, and `401 Unauthorized` is returned.
6. **Concurrent Refresh Protection:** Database lookups during rotation use pessimistic write locking (`SELECT ... FOR UPDATE`) to prevent concurrent race conditions from duplicating tokens.
7. **Stateless Access Token Disconnect:** Logout revokes the refresh session and clears the cookie immediately. However, already-issued short-lived access JWTs remain cryptographically valid until their expiration timestamp (`exp`).

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

---

### 2. User Login
`POST /api/v1/auth/login` (Public)

Authenticates user credentials, sets an HttpOnly `refresh_token` cookie, and returns a short-lived access JWT.

```bash
curl -i -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ada.lovelace@example.com",
    "password": "SecureP@ssword123"
  }'
```

**Response Headers:**
```http
Set-Cookie: refresh_token=n_4xP...; Path=/api/v1/auth; Max-Age=604800; HttpOnly; SameSite=Lax
```

**Response Body (`200 OK`):**
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

### 3. Refresh Access Token
`POST /api/v1/auth/refresh` (Public)

Consumes the `refresh_token` cookie, rotates it, sets a new `refresh_token` cookie, and returns a fresh access JWT.

```bash
curl -i -X POST http://localhost:8081/api/v1/auth/refresh \
  --cookie "refresh_token=n_4xP..."
```

**Response Body (`200 OK`):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { ... }
}
```

---

### 4. Logout (Current Session)
`POST /api/v1/auth/logout` (Public)

Revokes the active refresh token session in the database and clears the `refresh_token` cookie (`Max-Age=0`). Safe and idempotent.

```bash
curl -i -X POST http://localhost:8081/api/v1/auth/logout \
  --cookie "refresh_token=n_4xP..."
```

**Response Body (`200 OK`):**
```json
{
  "message": "Logged out successfully."
}
```

---

### 5. Logout All Sessions
`POST /api/v1/auth/logout-all` (Protected)

Requires `Authorization: Bearer <access_token>`. Revokes all active refresh token sessions belonging to the authenticated user and clears the cookie.

```bash
curl -X POST http://localhost:8081/api/v1/auth/logout-all \
  -H "Authorization: Bearer <your_jwt_access_token>"
```

**Response Body (`200 OK`):**
```json
{
  "message": "All active sessions have been logged out successfully."
}
```

---

### 6. User Profile Management
#### A. Retrieve Current Profile
`GET /api/v1/users/me` (Protected)

Requires `Authorization: Bearer <your_jwt_access_token>`. Resolves the user identity exclusively from the authenticated JWT security context.

```bash
curl -X GET http://localhost:8081/api/v1/users/me \
  -H "Authorization: Bearer <your_jwt_access_token>"
```

#### B. Update Current Profile
`PUT /api/v1/users/me` (Protected)

Requires `Authorization: Bearer <your_jwt_access_token>`. Allows updating safe profile fields (`firstName`, `lastName`). Automatically normalizes names and prevents mass-assignment of sensitive attributes (`role`, `email`, `accountStatus`, `id`, `passwordHash`).

```bash
curl -X PUT http://localhost:8081/api/v1/users/me \
  -H "Authorization: Bearer <your_jwt_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Ada Augusta",
    "lastName": "King-Noel"
  }'
```

---

### 7. Password Management
#### A. Change Password
`POST /api/v1/users/me/change-password` (Protected)
Requires authentication. Verifies current password with BCrypt, validates new password complexity, enforces password confirmation match, prohibits reusing the same password, updates the hash, and terminates all existing refresh sessions.

#### B. Forgot Password
`POST /api/v1/auth/forgot-password` (Public)
Accepts user email and dispatches a single-use SHA-256 hashed reset token valid for 15 minutes. Returns a generic response to prevent account enumeration.

#### C. Reset Password
`POST /api/v1/auth/reset-password` (Public)
Consumes the single-use reset token atomically with pessimistic write locking, updates the password, and invalidates all active refresh tokens.

---

### 8. Account Lifecycle
#### A. Verify Email
`POST /api/v1/auth/verify-email` (Public)
Consumes single-use verification token and activates inactive accounts.

#### B. Resend Verification
`POST /api/v1/auth/resend-verification` (Public)
Issues replacement verification token if account is unverified.

#### C. Self-Service Deactivation
`POST /api/v1/users/me/deactivate` (Protected)
Requires password confirmation. Sets account to `INACTIVE` and terminates all sessions.

#### D. Immediate JWT Revocation (Real-Time Enforcement)
`JwtAuthenticationFilter` validates user existence and active status on every authenticated request. Inactive or suspended accounts are immediately blocked with HTTP 403 Forbidden without waiting for access token expiration.

---

### 9. Admin User Management (Role: ADMIN Required)
- `GET /api/v1/admin/users`: Paged user listing with search, role, and account status filters.
- `GET /api/v1/admin/users/{userId}`: Retrieve detailed user profile.
- `PATCH /api/v1/admin/users/{userId}`: Safely update names.
- `PATCH /api/v1/admin/users/{userId}/role`: Update role with self-demotion prevention and last active administrator protection.
- `POST /api/v1/admin/users/{userId}/activate`: Administratively activate user.
- `POST /api/v1/admin/users/{userId}/deactivate`: Administratively deactivate user.
- `POST /api/v1/admin/users/{userId}/suspend`: Administratively suspend user with reason.
- `POST /api/v1/admin/users/{userId}/reactivate`: Restore suspended user to active.
- `DELETE /api/v1/admin/users/{userId}`: Soft-delete user (preserves audit records).
- `GET /api/v1/admin/audit-logs`: Paged audit log trail.

---

### 10. Postman Collection & Automated Verification
A complete Postman collection and environment are pre-configured in `postman/`:
- `postman/APIShield-User-Service.postman_collection.json`
- `postman/APIShield-User-Service.postman_environment.json`

Import both files into Postman. The collection includes automatic token capture scripts that save `accessToken`, `adminAccessToken`, and user identifiers into environment variables for seamless testing.

---

### 11. Actuator Health
`GET /actuator/health` (Public)

Health check endpoint. Returns `{"status": "UP"}`.

---

## How to Test and Run

### Run Tests
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
Ensure MySQL or TiDB Cloud is configured in environment variables or `.env`:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

---

## Current Implementation Status
**Steps 1 through 10: Complete Implementation**
- Registration, BCrypt password hashing, role restrictions, and email normalization.
- Stateless Spring Security with JWT (HMAC-SHA256) and HttpOnly refresh token cookies.
- Cryptographic refresh token rotation, pessimistic write locking, and automatic family reuse detection.
- Complete user profile management with strict mass-assignment protection.
- Password change and single-use password reset lifecycle with token hashing.
- Email verification, self-service deactivation, and real-time JWT revocation for inactive/suspended accounts.
- Administrative user management: pagination, filtering, role assignment, last-admin protection, soft-delete, and audit logging.
- 101 automated unit and integration tests passing with 100% success rate.
- Ready-to-import Postman collection and environment.
