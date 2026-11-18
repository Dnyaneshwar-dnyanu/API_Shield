# APIShield — Complete System Flows & Gateway Lifecycle

## 1. Complete System Workflows

This document outlines the end-to-end interactions across APIShield components, detailing each major operational flow with accompanying Mermaid sequence diagrams.

---

### Flow A: Owner Registration and API Publishing

An API Owner creates an account, logs in, registers their backend API, defines path endpoints, and configures monetization/usage plans.

```mermaid
sequenceDiagram
    autonumber
    actor Owner as API Owner
    participant Gateway as API Gateway
    participant UserSvc as User Service
    participant RegSvc as API Registry Service
    participant Redis as Redis Cache
    participant Kafka as Apache Kafka
    participant NotifSvc as Notification Service

    Owner->>Gateway: POST /api/v1/auth/register (Role: OWNER)
    Gateway->>UserSvc: Forward registration
    UserSvc->>UserSvc: Hash password (BCrypt), persist user
    UserSvc--)Kafka: Publish: user.registered
    UserSvc-->>Gateway: HTTP 201 Created (User DTO)
    Gateway-->>Owner: HTTP 201 Created
    Kafka--)NotifSvc: Consume: user.registered (Send welcome email)

    Owner->>Gateway: POST /api/v1/apis (Target URL, Slug, Name)
    Gateway->>RegSvc: Forward with Bearer JWT
    RegSvc->>RegSvc: Validate URL & enforce SSRF checks
    RegSvc->>RegSvc: Persist API record (Status: DRAFT)
    RegSvc-->>Gateway: HTTP 201 Created (API ID)
    Gateway-->>Owner: HTTP 201 Created

    Owner->>Gateway: POST /api/v1/apis/{id}/plans (Free & Pro tiers)
    Gateway->>RegSvc: Forward plan config
    RegSvc->>RegSvc: Persist plans (RPS, Quota, Price)
    RegSvc-->>Gateway: HTTP 201 Created

    Owner->>Gateway: PUT /api/v1/apis/{id}/publish
    Gateway->>RegSvc: Publish API request
    RegSvc->>RegSvc: Update status to PUBLISHED
    RegSvc->>Redis: Warm cache api:metadata:{slug}
    RegSvc--)Kafka: Publish: api.published
    RegSvc-->>Gateway: HTTP 200 OK
    Gateway-->>Owner: HTTP 200 OK (API Live on Marketplace)
```

---

### Flow B: Developer Registration and Authentication

A client developer signs up, authenticates, and obtains a JSON Web Token (JWT) containing their identity and assigned roles.

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer
    participant Gateway as API Gateway
    participant UserSvc as User Service

    Dev->>Gateway: POST /api/v1/auth/register (Role: DEVELOPER)
    Gateway->>UserSvc: Forward signup request
    UserSvc->>UserSvc: Validate email uniqueness, hash password
    UserSvc->>UserSvc: Persist user & assign ROLE_DEVELOPER
    UserSvc-->>Gateway: HTTP 201 Created
    Gateway-->>Dev: HTTP 201 Created

    Dev->>Gateway: POST /api/v1/auth/login (Email + Password)
    Gateway->>UserSvc: Authenticate credentials
    UserSvc->>UserSvc: Verify BCrypt hash
    UserSvc->>UserSvc: Generate Access Token (15m) & Refresh Token (7d)
    UserSvc-->>Gateway: Return JWT Tokens + User Info
    Gateway-->>Dev: HTTP 200 OK {accessToken, refreshToken}
```

---

### Flow C: API Marketplace and Discovery

Developers browse public, published APIs and inspect their documentation, available endpoints, and pricing plans.

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer
    participant Gateway as API Gateway
    participant RegSvc as API Registry Service

    Dev->>Gateway: GET /api/v1/catalog?category=AI&page=0
    Gateway->>RegSvc: Route request to Registry Service
    RegSvc->>RegSvc: Query published, public APIs with endpoints & plans
    RegSvc-->>Gateway: Return Paginated Catalog DTO
    Gateway-->>Dev: HTTP 200 OK (Catalog List)

    Dev->>Gateway: GET /api/v1/catalog/{slug}
    Gateway->>RegSvc: Fetch detailed specification
    RegSvc-->>Gateway: Return API details, endpoints, tiers
    Gateway-->>Dev: HTTP 200 OK (API Details)
```

---

### Flow D & K: API Subscription and Payment Activation

Handling both Free tier direct activation and Paid tier checkout via **Razorpay** (Orders API, Checkout Modal, and HMAC-SHA256 signature verification).

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer
    participant Gateway as API Gateway
    participant SubSvc as Subscription Service
    participant PaySvc as Payment Service
    participant Razorpay as Razorpay Payment Gateway
    participant Kafka as Apache Kafka
    participant NotifSvc as Notification Service

    Dev->>Gateway: POST /api/v1/subscriptions (apiId, planId)
    Gateway->>SubSvc: Forward request (JWT Auth)

    alt Free Plan
        SubSvc->>SubSvc: Create subscription (Status: ACTIVE)
        SubSvc--)Kafka: Publish: subscription.activated
        SubSvc-->>Gateway: HTTP 201 Created (Active Subscription)
        Gateway-->>Dev: HTTP 201 Created
    else Paid Plan
        SubSvc->>SubSvc: Create subscription (Status: PENDING)
        SubSvc->>PaySvc: Request Checkout Order (Feign)
        PaySvc->>Razorpay: POST /v1/orders (Create Razorpay Order)
        Razorpay-->>PaySvc: Return order_id, amount, currency
        PaySvc-->>SubSvc: Return order details
        SubSvc-->>Gateway: HTTP 200 OK {subscriptionId, orderId, razorpayKeyId}
        Gateway-->>Dev: Return order config for Razorpay Checkout Modal

        Dev->>Razorpay: Complete payment (UPI / Card / NetBanking)
        Razorpay->>Gateway: POST /api/v1/payments/webhook [X-Razorpay-Signature]
        Gateway->>PaySvc: Forward raw signed webhook
        PaySvc->>PaySvc: Verify HMAC-SHA256(payload, secret) == X-Razorpay-Signature
        PaySvc->>PaySvc: Verify payment.captured & record transaction
        PaySvc--)Kafka: Publish: payment.completed
        PaySvc-->>Gateway: HTTP 200 OK (Webhook Acknowledged)

        Kafka--)SubSvc: Consume: payment.completed
        SubSvc->>SubSvc: Transition status PENDING -> ACTIVE
        SubSvc--)Kafka: Publish: subscription.activated
        Kafka--)NotifSvc: Consume: subscription.activated (Email Receipt)
    end
```

---

### Flow E: API Key Generation

Developers generate a cryptographically secure API key for an active subscription.

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer
    participant Gateway as API Gateway
    participant KeySvc as API Key Service
    participant SubSvc as Subscription Service
    participant Redis as Redis Cache
    participant Kafka as Apache Kafka

    Dev->>Gateway: POST /api/v1/keys (subscriptionId, name)
    Gateway->>KeySvc: Forward request (JWT Auth)
    KeySvc->>SubSvc: Validate active subscription (REST)
    SubSvc-->>KeySvc: Subscription is ACTIVE (Plan limits returned)

    KeySvc->>KeySvc: Generate raw key: ash_live_<prefix>_<randomSecret>
    KeySvc->>KeySvc: Compute SHA-256 hash of raw key
    KeySvc->>KeySvc: Persist key record (prefix, hash, status: ACTIVE)

    KeySvc->>Redis: Set apikey:auth:{keyHash} (TTL: 5m) with plan limits
    KeySvc--)Kafka: Publish: apikey.generated

    KeySvc-->>Gateway: Return raw key EXACTLY ONCE
    Gateway-->>Dev: HTTP 201 Created {rawKey, prefix, expiresAt}
    Note over Dev: Developer copies and stores key securely.
```

---

### Flow F, G, H, I, J: Complete Gateway Request Execution Lifecycle

The runtime execution flow when an external client invokes an API via the Gateway:

```mermaid
sequenceDiagram
    autonumber
    actor Client as API Consumer
    participant Gateway as API Gateway
    participant Redis as Redis Cache
    participant Backend as External Target Backend
    participant Kafka as Apache Kafka
    participant Analytics as Analytics Service

    Client->>Gateway: GET /gateway/{apiSlug}/users/123 [Header: X-API-Key: ash_live_...]

    %% 1. Route & Key identification
    Gateway->>Gateway: 1. Extract apiSlug & compute SHA-256(X-API-Key)

    %% 2. Redis Key Auth & Rate Limit Check
    Gateway->>Redis: 2. Query apikey:auth:{keyHash}
    alt Key Missing or Invalid in Redis
        Redis-->>Gateway: Key not found
        Gateway->>Gateway: Query fallback internal Key Service
        alt Invalid Key
            Gateway-->>Client: HTTP 401 Unauthorized
        end
    end
    Redis-->>Gateway: Key Metadata (Status, RPS, Quota, subId, apiId)

    %% 3. Status Validation
    alt Key or Subscription Inactive
        Gateway-->>Client: HTTP 403 Forbidden
    end

    %% 4. Rate Limiting via Redis Lua
    Gateway->>Redis: 3. Eval Lua: INCR ratelimit:{keyId}:{sec} & check quota
    alt Rate Limit or Quota Exceeded
        Redis-->>Gateway: Rate limit rejected
        Gateway-->>Client: HTTP 429 Too Many Requests
    end
    Redis-->>Gateway: Rate limit OK (Remaining quota returned)

    %% 5. Target Route Resolution
    Gateway->>Redis: 4. Get api:metadata:{apiSlug}
    Redis-->>Gateway: Target Backend URL (https://target.com/api/v1)

    %% 6. Request Forwarding
    Gateway->>Gateway: 5. Strip /gateway/{apiSlug}, add X-Forwarded-* headers
    Gateway->>Backend: 6. Non-blocking HTTP GET https://target.com/api/v1/users/123
    Backend-->>Gateway: 7. HTTP 200 OK (Payload)

    %% 7. Return to Client
    Gateway-->>Client: 8. HTTP 200 OK (Payload + X-RateLimit-* headers)

    %% 8. Async Telemetry
    Gateway-)Kafka: 9. Async Fire-and-Forget: request.logged (status: 200, latency: 42ms)
    Kafka-)Analytics: Consume & Micro-batch Insert into MySQL
```

---

## 2. API Gateway Complete Request Lifecycle (Phase-by-Phase)

The Gateway request filter pipeline executes across nine distinct phases:

```
[ Phase 1: Receive ] ──> [ Phase 2: Identify Route ] ──> [ Phase 3: Validate API Key ]
                                                                   │
                                                                   ▼
[ Phase 6: Resolve Backend ] <── [ Phase 5: Rate Limit & Quota ] <── [ Phase 4: Validate Sub ]
        │
        ▼
[ Phase 7: Forward Request ] ──> [ Phase 8: Return Response ] ──> [ Phase 9: Async Telemetry ]
```

### Phase 1: Receive Developer Request
- Inspects incoming HTTP connection, establishes read timeout handler.
- Generates a globally unique `X-Correlation-ID` (UUID) if not already provided.
- Extracts `X-API-Key` header from request. If absent, halts pipeline and triggers 401 error flow.

### Phase 2: Identify API Route
- Parses URI path: `/gateway/{apiSlug}/**`.
- Extracts `{apiSlug}` to identify the target API.
- Re-writes path to strip the gateway prefix (e.g. `/gateway/weather-api/v1/current` becomes `/v1/current`).

### Phase 3: Validate API Key
- Hashes the extracted raw API key with SHA-256.
- Executes an $O(1)$ in-memory lookup in Redis: `apikey:auth:{keyHash}`.
- If cache-miss, performs a resilient fallback query (indexed with logarithmic complexity $O(\log N)$ against the database) to API Key Service and caches the result for 300 seconds.
- Confirms key `status == 'ACTIVE'`.

### Phase 4: Validate Subscription
- Checks subscription status embedded in key cache: `subscriptionStatus == 'ACTIVE'`.
- Verifies current timestamp falls within `[current_period_start, current_period_end]`.

### Phase 5: Check Rate Limits and Quotas
- Executes atomic Redis Lua script enforcing sliding-window requests per second (RPS).
- Checks monthly usage counter `quota:{subscriptionId}:{YYYY_MM}` against plan quota limit.
- If either check fails, halts pipeline immediately and returns HTTP 429 with `Retry-After` header.

### Phase 6: Retrieve Backend Routing Configuration
- Retrieves cached routing definition `api:metadata:{apiSlug}` from Redis.
- Resolves base target backend URL (e.g., `https://api.externalbackend.com`).
- Performs SSRF protection validation to ensure backend IP does not resolve to private/loopback address space.

### Phase 7: Forward Request to External Backend
- Dispatches non-blocking HTTP request via Project Reactor Netty `HttpClient`.
- Injects standard tracing and proxy headers:
  - `X-Correlation-ID`: Trace ID.
  - `X-Forwarded-For`: Original client IP.
  - `X-Forwarded-Host`: Original hostname.
- Connect timeout: 2,000 ms. Read timeout: 5,000 ms (or custom per-API timeout).
- Protected by Resilience4j Circuit Breaker.

### Phase 8: Return Response to Client
- Receives response stream from external backend.
- Attaches rate limit telemetry headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `X-Correlation-ID`).
- Flushes response body and HTTP status code directly to client.

### Phase 9: Publish Analytics Event Asynchronously
- Captures total roundtrip latency, HTTP status code, request size, response size, and timestamp.
- Emits an asynchronous, non-blocking telemetry event to Kafka topic `apishield.gateway.analytics`.
- Zero impact on client response latency.

---

## 3. Comprehensive Error Handling & RFC 7807 Format

All error responses generated by the API Gateway adhere to the **RFC 7807 Problem Details for HTTP APIs** specification:

```json
{
  "type": "https://apishield.io/errors/RATE_LIMIT_EXCEEDED",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "You have exceeded your plan's burst rate limit of 20 requests per second.",
  "instance": "/gateway/weather-api/v1/forecast",
  "timestamp": "2026-11-18T10:15:30Z",
  "correlationId": "8f3b610c-33b6-4b98-bc1c-59be610731f2"
}
```

### Error Scenarios & Gateway Action Matrix

| Failure Condition | HTTP Status | RFC 7807 Type Code | Gateway Action & Client Guidance |
| :--- | :--- | :--- | :--- |
| **Missing API Key Header** | `401 Unauthorized` | `MISSING_API_KEY` | Request missing `X-API-Key` header. Direct client to pass valid key. |
| **Invalid / Unrecognized Key** | `401 Unauthorized` | `INVALID_API_KEY` | Key hash not found in Redis or database. Client must check key accuracy. |
| **Revoked API Key** | `403 Forbidden` | `REVOKED_API_KEY` | Key exists but status is `REVOKED`. Client must generate a new key. |
| **Inactive Subscription** | `403 Forbidden` | `SUBSCRIPTION_INACTIVE` | Subscription is `PENDING`, `SUSPENDED`, or `CANCELLED`. Direct client to developer portal. |
| **Expired Subscription** | `403 Forbidden` | `SUBSCRIPTION_EXPIRED` | Billing period lapsed. Client must renew or update payment method. |
| **Burst Rate Limit Exceeded** | `429 Too Many Requests`| `RATE_LIMIT_EXCEEDED` | Exceeded plan's RPS threshold. Gateway returns `Retry-After: 1` header. |
| **Monthly Quota Exhausted** | `429 Too Many Requests`| `QUOTA_EXHAUSTED` | Monthly request limit consumed. Direct client to upgrade plan tier. |
| **Unavailable Target Backend** | `502 Bad Gateway` | `BACKEND_UNAVAILABLE` | External backend connection refused, DNS lookup failed, or SSL handshake error. |
| **Target Backend Timeout** | `504 Gateway Timeout`| `BACKEND_TIMEOUT` | External backend failed to respond within configured read timeout (e.g. 5s). |
| **Circuit Breaker Open** | `503 Service Unavailable`| `CIRCUIT_BREAKER_OPEN`| Downstream target backend has sustained high failure rate; requests short-circuited. |
| **SSRF Policy Violation** | `400 Bad Request` | `INVALID_BACKEND_URL` | Target backend resolved to a forbidden private IP address range. |
