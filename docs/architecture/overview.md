# APIShield — Architecture Overview

## 1. Executive Summary

**APIShield** is an enterprise-grade API Management and Security Platform built with Java 21, Spring Boot microservices, and React. The platform serves as a unified control plane and high-performance data plane for API monetization, governance, and consumption.

The system addresses two primary customer segments:
1. **API Owners (Providers):** Organizations or independent engineers who register their backend APIs, define monetization tiers (free, paid, subscription plans), establish rate limits and quotas, and observe real-time consumption telemetry and revenue.
2. **API Developers (Consumers):** Client developers who discover published APIs via a self-service marketplace, subscribe to plans, acquire cryptographically secure API keys, and consume backend APIs through a high-throughput, low-latency API Gateway.

---

## 2. Core Architectural Tenets

APIShield's architecture is guided by six foundational design principles:

### 2.1 Separation of Control Plane and Data Plane
- **Control Plane:** Comprises the User Service, API Registry Service, Subscription Service, Payment Service, API Key Service, Notification Service, and Analytics Service. These services handle business operations, authentication, payments, lifecycle workflows, and administrative dashboards. The Control Plane prioritizes strong transactional consistency and domain isolation.
- **Data Plane:** Centered entirely around the **Spring Cloud Gateway** and **Redis**. It is responsible for routing live consumer traffic to external backend servers. The Data Plane prioritizes sub-millisecond overhead, non-blocking I/O, distributed in-memory rate limiting, and zero direct dependencies on synchronous database lookups during runtime proxying.

### 2.2 Database-per-Service (Logical Isolation)
Every microservice maintains complete sovereignty over its data storage. Under no circumstances may one microservice execute SQL queries against or read directly from another microservice's database schema. Cross-service data interactions occur exclusively through explicit REST APIs or asynchronous event streams. Even when microservices initially share a single physical MySQL database instance, each service is assigned an isolated logical schema with dedicated credentials.

### 2.3 Event-Driven Asynchrony via Apache Kafka
State transitions with cross-boundary side effects (such as payments triggering subscription activations, subscription cancellations revoking API keys, or proxy events triggering analytics logging) are published as durable domain events to Apache Kafka. This guarantees eventual consistency, decouples latency-sensitive workflows, and prevents distributed cascading failures.

### 2.4 Distributed In-Memory Validation & Rate Limiting
To achieve high throughput on the Data Plane, API key verification, routing configuration, rate limiting, and quota enforcement are executed entirely within **Redis** via atomic Lua scripts. No synchronous relational database queries occur on the critical path of an API request passing through the API Gateway.

### 2.5 Centralized Service Discovery & Dynamic Configuration
Microservice discovery is orchestrated by **Spring Cloud Netflix Eureka**. Internal microservices dynamically register their network locations, enabling client-side load balancing and transparent horizontal autoscaling.

### 2.6 Zero-Trust Edge Security & SSRF Protection
The API Gateway acts as the single public entry point for both platform management and API proxying. Internal microservices operate in an isolated private network. External backend URLs registered by API owners undergo strict validation and defense-in-depth Server-Side Request Forgery (SSRF) checks prior to proxying.

### 2.7 Maven Multi-Module Architecture & Minimal Common Module
The backend is structured as a **Maven Multi-Module project** anchored at `backend/pom.xml`. To maintain strict service boundary independence while standardizing cross-boundary wire contracts, a minimal shared module `backend/common` provides:
- Universal Kafka event envelopes (`EventEnvelope<T>`).
- Standardized RFC 7807 problem details representations.
- Common security claim extractors and shared header constants (`X-Correlation-ID`, `X-Internal-Token`).
- Shared immutable DTOs for inter-service communication.
To preserve microservice sovereignty and avoid the "distributed monolith" anti-pattern, `backend/common` is strictly minimal: it contains zero business logic, zero database entities, and zero service-specific dependencies.

---

## 3. High-Level System Architecture

The following diagram illustrates the structural layout of APIShield, highlighting the distinct boundaries between external clients, edge ingress, control-plane microservices, data-plane caching, event streaming, and backing stores.

```mermaid
flowchart TD
    subgraph Clients["Clients & Consumers"]
        OwnerUI["API Owner (React Portal)"]
        DevUI["Developer Consumer (React Portal)"]
        ApiClient["API Client Application (cURL / SDK / App)"]
    end

    subgraph Ingress["Ingress & Edge Layer"]
        Gateway["Spring Cloud Gateway (Port: 8080)"]
    end

    subgraph Discovery["Service Discovery"]
        Eureka["Eureka Discovery Server (Port: 8761)"]
    end

    subgraph DataPlaneCache["Data Plane In-Memory Tier"]
        Redis[("Redis Cluster / Standalone (Port: 6379)\n- Key Auth Cache\n- Token Bucket Rate Limiting\n- Monthly Quotas\n- Routing Cache")]
    end

    subgraph ControlPlane["Control Plane Microservices (Java 21 / Spring Boot)"]
        UserService["User Service (Port: 8081)\n- Auth & RBAC (JWT)"]
        RegistryService["API Registry Service (Port: 8082)\n- API Specs & Plans"]
        SubService["Subscription Service (Port: 8083)\n- Subscriptions & Lifecycles"]
        ApiKeyService["API Key Service (Port: 8084)\n- Key Gen & Hashing"]
        PaymentService["Payment Service (Port: 8085)\n- Billing & Webhooks"]
        NotificationService["Notification Service (Port: 8086)\n- Emails & Alerts"]
        AnalyticsService["Analytics Service (Port: 8087)\n- Log Ingest & Rollups"]
    end

    subgraph EventStreaming["Event Streaming Infrastructure"]
        Kafka["Apache Kafka Broker (Port: 9092)\nTopics: apishield.*"]
    end

    subgraph Storage["Persistent Data Tier (MySQL Logical Schemas)"]
        DB_User[("apishield_user")]
        DB_Registry[("apishield_registry")]
        DB_Sub[("apishield_subscription")]
        DB_ApiKey[("apishield_apikey")]
        DB_Payment[("apishield_payment")]
        DB_Notification[("apishield_notification")]
        DB_Analytics[("apishield_analytics")]
    end

    subgraph Telemetry["Observability Tier"]
        Prometheus["Prometheus (Port: 9090)"]
        Grafana["Grafana (Port: 3000)"]
    end

    subgraph ExternalTargets["External Third-Party Ecosystem"]
        TargetBackends["External Target Backend APIs (e.g. Weather, Financial, AI APIs)"]
        PaymentGateway["Payment Gateway (Razorpay - Initial Provider)"]
    end

    %% Client Connections
    OwnerUI -->|HTTPS / REST Management| Gateway
    DevUI -->|HTTPS / REST Management| Gateway
    ApiClient -->|HTTPS /gateway/{slug}/* API Requests| Gateway

    %% Gateway Operations
    Gateway -.->|Register & Discover| Eureka
    Gateway <-->|Validate Key & Check Rate Limits| Redis
    Gateway -->|Forward Validated Request| TargetBackends
    Gateway -.->|Async Publish Request Telemetry| Kafka

    %% Control Plane Discovery
    UserService -.->|Register| Eureka
    RegistryService -.->|Register| Eureka
    SubService -.->|Register| Eureka
    ApiKeyService -.->|Register| Eureka
    PaymentService -.->|Register| Eureka
    NotificationService -.->|Register| Eureka
    AnalyticsService -.->|Register| Eureka

    %% Gateway to Control Plane Routing
    Gateway -->|Route /api/v1/auth/**| UserService
    Gateway -->|Route /api/v1/apis/**| RegistryService
    Gateway -->|Route /api/v1/subscriptions/**| SubService
    Gateway -->|Route /api/v1/keys/**| ApiKeyService
    Gateway -->|Route /api/v1/payments/**| PaymentService
    Gateway -->|Route /api/v1/analytics/**| AnalyticsService

    %% Control Plane to Storage
    UserService --> DB_User
    RegistryService --> DB_Registry
    SubService --> DB_Sub
    ApiKeyService --> DB_ApiKey
    PaymentService --> DB_Payment
    NotificationService --> DB_Notification
    AnalyticsService --> DB_Analytics

    %% Kafka Event Connections
    PaymentService -.->|Publish: payment.completed| Kafka
    SubService -.->|Publish: subscription.activated| Kafka
    SubService -.->|Consume: payment.completed| Kafka
    ApiKeyService -.->|Consume: subscription.activated| Kafka
    ApiKeyService -.->|Sync Active Key Hashes| Redis
    RegistryService -.->|Sync Route Config| Redis
    Kafka -.->|Consume: Telemetry Events| AnalyticsService
    Kafka -.->|Consume: User/Billing Events| NotificationService

    %% Third-party integrations
    PaymentService <-->|Process Charges & Webhooks| PaymentGateway

    %% Observability
    Gateway -.->|Scrape /actuator/prometheus| Prometheus
    ControlPlane -.->|Scrape /actuator/prometheus| Prometheus
    Prometheus --> Grafana
```

---

## 4. Key Stakeholder Personas & Interaction Models

| Persona | Core Needs | Primary Touchpoints | Applicable Roles |
| :--- | :--- | :--- | :--- |
| **API Owner** | Register APIs, configure target backend URLs, define subscription tiers (free, paid quotas), inspect subscriber counts, view uptime, and monitor earned revenue. | React Owner Console via `/api/v1/apis/**`, `/api/v1/analytics/owner/**` | `ROLE_OWNER` |
| **API Developer** | Browse marketplace catalog, subscribe to APIs, generate and revoke API keys, inspect usage quotas, invoke APIs via `/gateway/{apiSlug}/**`. | React Marketplace Portal via `/api/v1/catalog/**`, `/api/v1/subscriptions/**`, `/api/v1/keys/**` | `ROLE_DEVELOPER` |
| **Platform Administrator**| Oversee system health, manage tenant disputes, inspect system-wide traffic, suspend abusive accounts, and monitor microservices telemetry. | React Admin Console via `/api/v1/admin/**`, Grafana dashboards | `ROLE_ADMIN` |

---

## 5. Technology Stack & Architectural Justification

| Layer / Component | Technology | Rationale & Architectural Justification |
| :--- | :--- | :--- |
| **Language & Runtime** | Java 21 LTS | Leverages modern Java language features (Virtual Threads / Project Loom for lightweight concurrency, Record classes for immutable DTOs, Pattern Matching, enhanced performance and security). |
| **Build System & Modules** | Maven Multi-Module (`backend/pom.xml`) | Orchestrates centralized dependency management (`<dependencyManagement>`), eliminates library version skew, and coordinates submodules including the minimal `backend/common` module. |
| **Framework** | Spring Boot 3.3+ | Industry-standard microservices framework with native cloud-native capabilities, comprehensive dependency injection, declarative validation, and mature data access integrations. |
| **Edge Gateway** | Spring Cloud Gateway | Non-blocking, reactive HTTP gateway built on Project Reactor and Netty. Capable of handling tens of thousands of concurrent connections with low memory footprint and high throughput. |
| **Service Discovery** | Spring Cloud Netflix Eureka | Dynamic registry providing seamless client-side load balancing via Spring Cloud LoadBalancer, avoiding static IP configuration in containerized environments. |
| **Database** | MySQL 8.0+ | ACID-compliant relational storage ensuring rigorous transactional integrity for financial transactions, billing records, and subscription state machines. |
| **Caching & Rate Limiting** | Redis 7.x | High-speed in-memory key-value data store. Serves sub-millisecond API key hash lookups, atomic sliding-window counter rate limiting, and monthly quota counters. |
| **Event Streaming** | Apache Kafka 3.x | Distributed append-only commit log providing decoupled, resilient asynchronous messaging, horizontal partition scalability, and replayable event auditing. |
| **Payment Provider** | Razorpay (Initial Provider) | Initial payment gateway handling checkout orders (Razorpay Orders API), payments, and HMAC-SHA256 signature-verified webhook state transitions for paid subscriptions. |
| **Resilience** | Resilience4j | Lightweight fault tolerance library providing Circuit Breaker, RateLimiter, TimeLimiter, and Retry capabilities for inter-service communication. |
| **Frontend** | React 18+ (SPA) | Component-driven user interface delivering an interactive developer marketplace, real-time analytics graphs, and administrative workflows. |
| **Observability** | Prometheus & Grafana | Pull-based timeseries metric scraping from Spring Boot Actuator endpoints, visualized in Grafana dashboards for latency (p50, p95, p99), error rates, and JVM memory. |
| **Containerization** | Docker & Docker Compose | Standardized OCI container runtime enabling reproducible local development, staging environments, and unified deployment descriptors. |

---

## 6. Architectural Quality Attributes

### 6.1 Scalability
- **Horizontal Scaling:** The API Gateway and all microservices are stateless. Any service instance can be multiplied horizontally behind a load balancer or container orchestrator.
- **Partitioned Streaming:** Kafka topics are partitioned based on natural partition keys (`api_id`, `developer_id`), allowing consumer groups to scale linearly with traffic volume.
- **Cache Offloading:** Relational database load is shielded by Redis caching layer, keeping read latency flat even under traffic surges.

### 6.2 Availability & Fault Tolerance
- **Graceful Degradation:** If the Analytics Service or Kafka broker experiences transient downtime, the API Gateway buffers telemetry locally or drops metrics without interrupting live proxy requests to external target backends.
- **Circuit Breaking:** Outbound calls from the API Gateway to third-party target backends are wrapped in Resilience4j circuit breakers, preventing cascading thread starvation when external backends fail.

### 6.3 Security & Zero-Trust
- All ingress traffic is authenticated and authorized at the edge.
- Client applications access external APIs using opaque, high-entropy API keys that are never stored in plaintext.
- Strict SSRF protection prevents registered target backends from targeting internal Docker networks or loopback interfaces.

---

## 7. Document Index

For deep-dive technical specifications, refer to the following documents:
- [Microservices Architecture Specification](file:///d:/Notes/SEM%20VII/Microservices/API_Shield/docs/architecture/microservices.md)
- [Database Design & Schema Specification](file:///d:/Notes/SEM%20VII/Microservices/API_Shield/docs/architecture/database-design.md)
- [Service Communication & Caching Strategy](file:///d:/Notes/SEM%20VII/Microservices/API_Shield/docs/architecture/communication.md)
- [Complete System Flows & Gateway Lifecycle](file:///d:/Notes/SEM%20VII/Microservices/API_Shield/docs/architecture/request-flows.md)
- [Security Architecture & Threat Mitigation](file:///d:/Notes/SEM%20VII/Microservices/API_Shield/docs/architecture/security.md)
