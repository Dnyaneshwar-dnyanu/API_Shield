# APIShield End-to-End (E2E) Integration & Benchmarking Suite

This test suite performs comprehensive automated end-to-end integration testing and latency benchmarking across the entire APIShield microservices platform.

---

## 📋 Verified Business Journey Steps

| Step | Scope | Key Assertions & Behaviors |
| :--- | :--- | :--- |
| **Step 1** | **Cluster Health Probe** | Actuator `/actuator/health` across Discovery Server (8761), Edge Gateway (8080), and all 7 domain microservices. |
| **Step 2** | **Identity & JWT Auth** | Registers API Owner (`API_OWNER`) and Developer (`API_CONSUMER`); issues signed JWT Bearer tokens. |
| **Step 3** | **API Registration & Pricing** | Owner registers API (`weather-api`), creates Free (5 RPS) and Pro (100 RPS) plans, and publishes to catalog. |
| **Step 4** | **Marketplace Subscription** | Developer browses marketplace catalog and subscribes to Free Plan; creates `ACTIVE` subscription. |
| **Step 5** | **Cryptographic API Key** | Generates raw API key starting with `ash_live_`; stores SHA-256 hash in MySQL and caches metadata in Redis. |
| **Step 6** | **9-Phase Gateway Proxy** | Invokes `GET /gateway/weather-api/current` through Gateway; validates API Key, rate limit, quota, proxies to upstream. |
| **Step 7** | **Rate Limiting (429)** | Fires burst requests exceeding 5 req/sec; asserts RFC 7807 `429 Too Many Requests` problem details. |
| **Step 8** | **Kafka Events & Notifications**| Verifies event delivery to Kafka topics and retrieval of user alerts via Notification Service. |
| **Step 9** | **Real-Time Analytics** | Verifies Kafka telemetry ingestion by Analytics Service; validates owner overview and developer quota. |
| **Step 10**| **Latency Benchmarking** | Measures P50, P90, P95, P99 latency percentiles and throughput (requests/sec) through the Edge Gateway. |

---

## 🚀 Execution Instructions

### Running in Standalone Contract Mode
Validates all protocol contracts, assertion logic, and benchmark calculations instantly without running microservices:
```powershell
npm --prefix tests/e2e run test:mock
# or
powershell -ExecutionPolicy Bypass -File tests/e2e/e2e-business-journey.ps1 -Mode mock
```

### Running against Live Infrastructure & Microservices
Ensure Docker Compose is running (`docker compose -f infrastructure/docker/docker-compose.yml up -d`):
```powershell
npm --prefix tests/e2e run test:live
# or
powershell -ExecutionPolicy Bypass -File tests/e2e/e2e-business-journey.ps1 -Mode live
```

### Dedicated Gateway Benchmarking
```powershell
node tests/e2e/benchmark.js http://localhost:8080/gateway/weather-api/current ash_live_your_key 100 10
```
