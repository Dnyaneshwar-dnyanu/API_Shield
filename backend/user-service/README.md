# User Service

## Purpose
The User Service is a core microservice within the APIShield platform. It is responsible for user identity management, account registration (API Owners, Developers, Admins), credential handling, role-based access control (RBAC), and authentication token generation (JWT).

## Technology Stack
- **Language:** Java 21 LTS
- **Framework:** Spring Boot 4.1.2
- **Data Persistence:** Spring Data JPA, MySQL Connector/J
- **Security:** Spring Security
- **Validation:** Spring Boot Starter Validation
- **Monitoring:** Spring Boot Actuator
- **Productivity:** Project Lombok
- **Build Tool:** Apache Maven (via Maven Wrapper `mvnw`)

## Prerequisites
- Java Development Kit (JDK) 21 LTS installed and configured in `JAVA_HOME`.
- Access to terminal or command prompt (PowerShell / Bash).

## How to Run

### Build the Service
From the `backend/user-service` directory:
```bash
# Windows
.\mvnw.cmd clean package

# Linux / macOS
./mvnw clean package
```

### Run the Service
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

The service starts on port **8081**.
Health endpoint is exposed at: `http://localhost:8081/actuator/health`

## Current Implementation Status
**Project Initialization (Step 2)**
- Spring Boot application structure initialized.
- Dependencies and plugins configured in `pom.xml`.
- Application configured on port 8081 with actuator health endpoints.
- Database auto-configuration safely deferred until persistent schema and entities are introduced.
- Authentication and business logic are not yet implemented.
