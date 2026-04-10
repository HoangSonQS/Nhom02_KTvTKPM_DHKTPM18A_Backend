# SEBook Modular Monolith Backend — Nhom02 DHKTPM18A

[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](https://github.com/your-username/sebook-backend/actions)
[![Java Version](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Architecture](https://img.shields.io/badge/Architecture-Modular%20Monolith-blue.svg)](#2-architecture)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**SEBook** is a comprehensive bookstore management system built with Hexagonal Architecture, Strict DDD, and modern cloud-native principles.

---

## 📑 Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Tech Stack](#3-tech-stack)
4. [Prerequisites](#4-prerequisites)
5. [Getting Started](#5-getting-started)
6. [Environment Variables](#6-environment-variables)
7. [API Documentation](#7-api-documentation)
8. [Module Structure](#8-module-structure)
9. [Database & Migrations](#9-database--migrations)
10. [Security Strategy](#10-security-strategy)
11. [Testing Strategy](#11-testing-strategy)
12. [Key Business Flows](#12-key-business-flows)
13. [Resilience & Performance](#13-resilience--performance)
14. [Known Limitations](#14-known-limitations)
15. [Phase History (Changelog)](#15-phase-history-changelog)
16. [Contributing](#16-contributing)
17. [License](#17-license)

---

## 1. Overview

SEBook provides a scalable and maintainable solution for online bookstores, featuring real-time inventory management, automated logistics, and a secure checkout experience.

The project is designed as a **Modular Monolith** to balance development speed with architectural cleanliness, making it straightforward to decompose into microservices if needed in the future.

**Key Design Principles:**
- **Hexagonal Architecture (Ports & Adapters)** — business logic is fully decoupled from infrastructure
- **Strict DDD** — each module has its own domain, enforced boundaries, and no cross-module entity sharing
- **Event-Driven Consistency** — cross-module side effects go through Transactional Outbox, not direct calls

---

## 2. Architecture

### 2.1 Hexagonal Architecture (Per Module)

```
+--------------------------------------------------+
|               Adapter Layer (Inbound)            |
|        REST Controllers | Event Listeners        |
+--------------------------------------------------+
|               Application Layer                  |
|     Input Ports (Interfaces) | Use Cases         |
|     Output Ports (Interfaces)                    |
+--------------------------------------------------+
|                 Domain Layer                     |
|      Entities | Value Objects | Business Rules   |
+--------------------------------------------------+
|              Adapter Layer (Outbound)            |
|   JPA Persistence | Redis Cache | Brevo Email    |
+--------------------------------------------------+
```

### 2.2 Module Communication Strategy

| Communication Type | When to Use | Example |
| :--- | :--- | :--- |
| **Direct Internal Call** | Same module or non-cyclic sync dependency | `CartService` → `CatalogInternalUseCase` |
| **Transactional Outbox** | Cross-module side effects requiring consistency | `Logistics` → `Inventory` stock update |
| **Domain Event (In-Process)** | Loose coupling within bounded context | `StockAdjustmentConfirmedEvent` |

### 2.3 Module Dependency Graph

```
auth ──────────────────────────────► (no dependencies)
catalog ───────────────────────────► (no dependencies)
inventory ─────────────────────────► catalog (read-only)
cart ──────────────────────────────► catalog, inventory
order ─────────────────────────────► cart, inventory, promotion
payment ───────────────────────────► order
promotion ─────────────────────────► catalog
logistics ─────────────────────────► inventory (via Outbox Event)
report ────────────────────────────► order, inventory, logistics (read-only)
```

---

## 3. Tech Stack

| Layer | Technology |
| :--- | :--- |
| Framework | Spring Boot 3.5.6 (Java 17) |
| Security | Spring Security + JWT (JJWT) + Redis Session |
| Database | PostgreSQL 15+ |
| Migration | Flyway |
| Cache / Session | Redis 7+ |
| Email | Brevo (SMTP / HTTP API) |
| Image Storage | Cloudinary |
| Resilience | Resilience4j + Spring Retry |
| Observability | Spring Actuator + Micrometer (Prometheus) |
| Testing | JUnit 5 + Mockito + Testcontainers |

---

## 4. Prerequisites

| Tool | Version | Notes |
| :--- | :--- | :--- |
| JDK | 17 | Mandatory |
| Maven | 3.8+ | Mandatory |
| Docker & Docker Compose | Latest | For PostgreSQL + Redis |
| PostgreSQL | 15+ | Port 5433 (default) |
| Redis | 7+ | Port 6379 (default) |

---

## 5. Getting Started

### Step 1 — Clone Repository

```bash
git clone https://github.com/your-username/sebook-backend.git
cd Nhom02_KTvTKPM_DHKTPM18A_Backend
```

### Step 2 — Start Infrastructure

```bash
docker-compose up -d
```

This starts PostgreSQL and Redis. Flyway migrations run automatically on application startup.

### Step 3 — Configure Environment

Copy and edit the environment file:

```bash
cp .env.example .env
# Edit .env with your values
```

### Step 4 — Run Application

```bash
mvn spring-boot:run
```

Application available at: `http://localhost:8080`

---

## 6. Environment Variables

| Variable | Description | Default | Mandatory |
| :--- | :--- | :--- | :--- |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5433/sebook` | Yes |
| `DB_NAME` | Database name (if using default URL) | `sebook` | No |
| `DB_USERNAME` | Database username | `postgres` | Yes |
| `DB_PASSWORD` | Database password | `postgres` | Yes |
| `JWT_SECRET_KEY` | Secret for signing JWT | *(none)* | Yes |
| `JWT_ACCESS_EXPIRY` | Access token TTL (ms) | `3600000` (1 hour) | No |
| `JWT_REFRESH_EXPIRY` | Refresh token TTL (ms) | `2592000000` (30 days) | No |
| `REDIS_HOST` | Redis server host | `localhost` | No |
| `REDIS_PORT` | Redis server port | `6379` | No |
| `BREVO_API_KEY` | Brevo email API key | *(none)* | Yes |
| `BREVO_SENDER_EMAIL` | Email sender address | *(none)* | Yes |
| `CLOUDINARY_URL` | Cloudinary storage URL | *(none)* | Yes |

---

## 7. API Documentation

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: `http://localhost:8080/api-docs`

### Base URL

All endpoints are versioned:

```
/api/v1/{module}/...
```

### Endpoint Groups

| Group | Base Path | Access |
| :--- | :--- | :--- |
| Authentication | `/api/v1/auth` | Public / Authenticated |
| Catalog | `/api/v1/catalog` | Public (read) / Staff (write) |
| Cart | `/api/v1/cart` | Customer |
| Orders | `/api/v1/orders` | Customer / Staff |
| Payments | `/api/v1/payments` | Customer |
| Account | `/api/v1/account` | Authenticated |
| Logistics | `/api/v1/logistics` | Staff / Admin |
| Admin | `/api/v1/admin` | Admin only |

---

## 8. Module Structure

Each module follows the same internal structure:

```
modules/
    └── {module_name}/
        ├── adapter/
        │   ├── inbound/
        │   │   ├── web/          # REST Controllers, Request/Response DTOs
        │   │   └── event/        # Event Listeners (Outbox consumers)
        │   └── outbound/
        │       ├── persistence/  # JPA Entities, Repositories, Adapters
        │       ├── cache/        # Redis Adapters
        │       └── external/     # Third-party clients (Brevo, Cloudinary)
        ├── application/
        │   ├── port/
        │   │   ├── in/           # Input Port Interfaces (Use Cases)
        │   │   └── out/          # Output Port Interfaces (Repositories, etc.)
        │   └── service/          # Use Case Implementations
        └── domain/               # Aggregate Roots, Entities, Value Objects
            └── event/            # Domain Events
```

### Module Responsibilities

| Module | Responsibility |
| :--- | :--- |
| `auth` | JWT lifecycle, RBAC, Redis session, Device management |
| `catalog` | Book and category management, search |
| `cart` | Shopping cart, item validation |
| `order` | Order lifecycle, status management |
| `payment` | Payment initiation, callback handling |
| `promotion` | Voucher validation and approval workflow |
| `inventory` | Stock levels, Outbox-based stock updates |
| `logistics` | Suppliers, Purchase Orders, Stock Adjustments |
| `report` | Admin dashboard read models (CQRS) |

---

## 9. Database & Migrations

- **Migration Tool**: Flyway (auto-runs on startup)
- **Location**: `src/main/resources/db/migration`
- **Naming**: `V{version}__{description}.sql`

### Table Prefix Convention

| Prefix | Module |
| :--- | :--- |
| `auth_` | Authentication, sessions |
| `cat_` | Catalog (books, categories) |
| `ord_` | Orders |
| `inv_` | Inventory |
| `log_` | Logistics (PO, suppliers) |
| `promo_` | Promotions |
| `pay_` | Payments |

### Manual Repair

```bash
mvn flyway:repair
```

---

## 10. Security Strategy

### 10.1 Authentication Flow (Per-Device)

```
LOGIN
  Client → POST /api/v1/auth/login + X-Device-ID
  Server → accessToken + csrfToken (Response Body)
         → refreshToken (HttpOnly Cookie, SameSite=Lax)
         → deviceId (Response Body, stored in localStorage)

API REQUEST
  Client → Authorization: Bearer <accessToken>
         → X-CSRF-Token: <csrfToken>
         → X-Device-ID: <deviceId>
         → Cookie: refresh_token (auto)

SILENT REFRESH (on 401)
  Client → POST /api/v1/auth/refresh + X-Device-ID
  Server → Verify rv_token == rv_redis (per device)
         → If match: rv++, issue new token pair
         → If mismatch: SECURITY ALERT, revoke device session

LOGOUT
  Server → Revoke Redis session + Clear Cookie
  Client → Clear memory (accessToken, csrfToken, deviceId)
```

### 10.2 Redis Session Key Structure

```
refresh:{userId}:{deviceId}          → refreshToken value  (TTL 7d)
refresh_version:{userId}:{deviceId}  → rv integer counter  (TTL 7d, renewed on each refresh)
```

### 10.3 RBAC Permission Matrix

| Domain | ADMIN | STAFF_SELLER | STAFF_WAREHOUSE | CUSTOMER | GUEST |
| :--- | :---: | :---: | :---: | :---: | :---: |
| Auth & Account | 10 | 5 | 5 | 5 | 2 |
| Catalog | 6 | 5 | 2 | 2 | 2 |
| Cart & Order | 8 | 2 | 0 | 6 | 0 |
| Payment | 3 | 1 | 0 | 2 | 0 |
| Promotion | 6 | 4 | 0 | 1 | 0 |
| Inventory | 11 | 4 | 10 | 0 | 0 |
| Return | 4 | 2 | 0 | 2 | 0 |
| Dashboard & Support | 5 | 1 | 1 | 0 | 0 |
| **Total** | **53** | **24** | **18** | **19** | **4** |

> `ADMIN` uses `EnumSet.allOf(Permission.class)` — currently **66 permissions** in total.

### 10.4 Security Hardening

- **HttpOnly Cookie** — `refreshToken` inaccessible to JavaScript (XSS protection)
- **SameSite=Lax** — CSRF mitigation for cookie-based refresh
- **X-CSRF-Token header** — required on all mutating requests (POST, PUT, PATCH, DELETE)
- **Reuse Detection** — token version counter (`rv`) detects stolen token replay
- **Fail-Closed** — Redis unavailable returns `503`, no stateless fallback
- **Rate Limiting** — `/api/v1/auth/refresh` protected against brute force (Bucket4j)

---

## 11. Testing Strategy

### Unit Tests

Test domain logic and application services in isolation. All external dependencies mocked via Mockito.

```bash
mvn test
```

### Integration Tests

Test persistence adapters and REST endpoints.

```bash
mvn verify -Pintegration
```

### Key Test Cases

| Test Class | What It Verifies |
| :--- | :--- |
| `POStateTransitionTest` | All valid and invalid PO state transitions |
| `InventoryAtomicUpdateTest` | Concurrent stock updates produce correct final count |
| `OutboxPatternIntegrationTest` | Event republished automatically after publish failure |
| `InventoryIdempotencyTest` | Duplicate eventId does not double-update stock |
| `ReuseDetectionTest` | Old refresh token (rv n-1) triggers session revocation |
| `PerDeviceSessionTest` | Device A refresh does not invalidate Device B session |

---

## 12. Key Business Flows

### 12.1 Authentication — Silent Refresh

```
Browser                    Server                   Redis
  |                           |                       |
  |-- POST /auth/login -----> |                       |
  |   + X-Device-ID           |-- save rv=1 --------> |
  |                           |-- save refreshToken ->|
  |<-- accessToken  --------- |                       |
  |<-- csrfToken    --------- |                       |
  |<-- Set-Cookie   --------- |                       |
  |   (refreshToken,          |                       |
  |    HttpOnly)              |                       |
  |                           |                       |
  |-- API Request ----------> |                       |
  |   Bearer + X-CSRF-Token   |-- verify JWT -------> |
  |                           |                       |
  |<-- 401 Unauthorized ----- |  (token expired)      |
  |                           |                       |
  |-- POST /auth/refresh ---->|                       |
  |   + X-Device-ID           |-- check rv_token  --> |
  |                           |   vs rv_redis         |
  |                           |-- INCR rv ----------> |
  |                           |-- EXPIRE keys ------> |
  |<-- new accessToken -------|                       |
  |<-- new Set-Cookie --------|                       |
```

### 12.2 Purchase Order Lifecycle

```
State Machine (enforced in PurchaseOrder domain entity):

  [DRAFT] ──────────────────────────────────────────► [CANCELLED]
     |       (by creator or ADMIN)                         ▲
     | submit()                                            |
     ▼       (by creator)                                  |
  [SUBMITTED] ──────────────────────────────────────► [CANCELLED]
     |    |     (by ADMIN or STAFF_WAREHOUSE)               ▲
     |    | return to draft                                 |
     |    └──────────────────► [DRAFT]                      |
     | approve()                                            |
     ▼       (by ADMIN)                                     |
  [APPROVED] ──────────────────────────────────────► [CANCELLED]
     |              (ONLY by ADMIN)
     | receive()
     ▼       (by STAFF_WAREHOUSE)
  [RECEIVED]
```

### 12.3 Inventory Update via Transactional Outbox

```
STAFF_WAREHOUSE confirms StockAdjustment
         |
         | (single @Transactional)
         |
         +──► Update StockAdjustment status → CONFIRMED
         |
         +──► INSERT into log_outbox_event (status = PENDING)
         |
         | DB COMMIT (both rows committed atomically)
         |
OutboxPublisherJob (runs every 5s)
         |
         +──► SELECT * FROM log_outbox_event WHERE status = 'PENDING'
         |
         +──► Publish StockAdjustmentConfirmedEvent
         |
InventoryLogisticsListener
         |
         +──► Check inv_processed_event (idempotency guard)
         |
         +──► inventoryUseCase.adjustStock(bookId, delta)
         |    (Atomic SQL: UPDATE inv_stock SET quantity = quantity + ?)
         |
         +──► INSERT into inv_processed_event (eventId)
         |
         +──► UPDATE log_outbox_event SET status = 'PUBLISHED'
```

---

## 13. Resilience & Performance

| Mechanism | Implementation | Purpose |
| :--- | :--- | :--- |
| Circuit Breaker | Resilience4j | Isolate failures in email / payment providers |
| Retry | Spring Retry | Auto-retry transient failures (network, DB) |
| Atomic Stock Update | `UPDATE inv_stock SET quantity = quantity + ? WHERE book_id = ?` | Race-condition-safe inventory changes |
| Optimistic Locking | `@Version` on `PurchaseOrder` | Prevent double-approve by concurrent admins |
| Rate Limiting | Bucket4j on `/api/v1/auth/refresh` | Brute force protection |
| Outbox Pattern | `log_outbox_event` + scheduled publisher | Guaranteed at-least-once event delivery |
| Idempotent Consumer | `inv_processed_event` table | Prevent duplicate stock updates |

---

## 14. Known Limitations

| Limitation | Detail | Planned Fix |
| :--- | :--- | :--- |
| Partial PO Receipt | System only supports full receipt (all or nothing). Partial delivery not tracked. | Phase 8+ |
| Concurrent Refresh Race | Two simultaneous refresh calls from same device may trigger false reuse detection. | Phase 9 (Distributed Lock via Redis SET NX) |
| RS256 JWT | Currently using HS256 (symmetric). RS256 needed only when splitting into microservices. | Phase 9 |
| Search Engine | Using PostgreSQL Full-Text Search. Elasticsearch / Meilisearch not yet integrated. | Phase 8 |
| Distributed Tracing | No request tracing across modules. | Phase 9 (Jaeger / Micrometer Tracing) |
| Outbox for PO→Inventory | PO RECEIVED → Inventory increase goes through Outbox. If outbox job is down, stock lags by up to 5s but will self-heal. | Acceptable, monitored via audit trail |

---

## 15. Phase History (Changelog)

| Phase | Scope | Status |
| :--- | :--- | :--- |
| Phase 1–3 | Project scaffolding, Auth module, Catalog, Cart | Done |
| Phase 4 | Order module, Mock Payment integration | Done |
| Phase 5 | Inventory sync, Transactional Outbox foundation | Done |
| Phase 5.5 | Security hardening: Redis session, Per-device auth, RBAC 2.0 (48 permissions), API versioning `/api/v1/` | Done |
| Phase 6 | Logistics module: Supplier, Purchase Order (full state machine), Stock Adjustment, Audit Trail, Price Snapshot, Outbox integration | **Current** |
| Phase 7 | Reverse Logistics & Customer Service (Returns) | Planned |
| Phase 8 | Advanced Intelligence: Semantic Search (pgvector), OCR Vision, Chatbot integration | Done |
| Phase 8.5 | Refactor: Book Atribute, Architecture Refactoring: Hexagonal Events, Strict DDD boundaries, Transactional Outbox synchronization | Done |
a| Phase 9 | System Hardening & Optimization | Planned |

---

## 16. Contributing

### Branch Naming

| Type | Pattern | Example |
| :--- | :--- | :--- |
| Feature | `feat/{module}/{description}` | `feat/logistics/purchase-order-state-machine` |
| Bug Fix | `fix/{module}/{description}` | `fix/auth/refresh-token-race-condition` |
| Refactor | `refactor/{module}/{description}` | `refactor/inventory/atomic-update` |

### Commit Format

Following [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(logistics): implement PO state machine with audit trail
fix(auth): correct rv counter scope to per-device
refactor(inventory): replace direct call with outbox event
```

### Pull Request Process

1. Minimum **1 approval** required before merge
2. All tests must pass (`mvn test`)
3. No reduction in code coverage
4. Update `Known Limitations` if introducing technical debt intentionally

---

## 17. License

Licensed under the [MIT License](LICENSE).