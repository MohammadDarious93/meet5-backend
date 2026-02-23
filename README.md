# Social Network Backend (Spring Boot + JDBC)

Production-style backend assignment implementing a social network interaction system with:
- user profiles
- profile visits and likes
- fraud detection rule
- bulk data insertion
- clean layered architecture

The project intentionally uses **plain JDBC + SQL** (no Hibernate/JPA) for explicit SQL control, predictable performance, and clear data access behavior.

## Tech Stack

- Java 17
- Spring Boot 3
- MySQL 8
- Spring JDBC (`JdbcTemplate`, `NamedParameterJdbcTemplate`)
- Maven
- JUnit 5 + Mockito

## Zero-Setup Review (Recommended)

This repository is set up for predictable local review with Docker + Flyway.

Requirements:
- Docker
- Java 17

Start database:
```bash
docker compose up -d
```

Run application:
```bash
./mvnw spring-boot:run
```

Why this is zero-setup:
- pinned DB image (`mysql:8.0`) via `docker-compose.yml`
- fixed DB credentials and schema name in local defaults
- Flyway automatically creates/version-controls schema on startup
- no manual SQL setup required

Optional health check:
- `http://localhost:8080/actuator/health`
- health endpoint is provided by Spring Boot Actuator included in this project

## Project Structure

```text
src/main/java/com/meet5/socialnetwork
  ├── config
  │   └── AppConfig.java
  ├── controller
  │   ├── GlobalExceptionHandler.java
  │   ├── InteractionController.java
  │   └── UserController.java
  ├── domain
  │   ├── ProfileVisitorView.java
  │   └── UserProfile.java
  ├── dto
  │   ├── ApiErrorResponse.java
  │   ├── BulkInsertResult.java
  │   ├── BulkUserInsertRequest.java
  │   ├── CreateUserRequest.java
  │   ├── LikeRequest.java
  │   ├── InteractionResponse.java
  │   ├── UserResponse.java
  │   └── VisitRequest.java
  ├── exception
  │   ├── BadRequestException.java
  │   ├── ConflictException.java
  │   └── NotFoundException.java
  ├── repository
  │   ├── InteractionRepository.java
  │   ├── JdbcInteractionRepository.java
  │   ├── JdbcUserRepository.java
  │   └── UserRepository.java
  ├── service
  │   ├── FraudDetectionService.java
  │   ├── InteractionService.java
  │   └── UserService.java
  └── SocialNetworkApplication.java

src/main/resources
  ├── application.yml
  └── db
      └── migration
          └── V1__initial_schema.sql

docker-compose.yml
mvnw
mvnw.cmd

src/test/java/com/meet5/socialnetwork
  ├── domain/UserProfileTest.java
  └── service
      ├── FraudDetectionServiceTest.java
      └── InteractionServiceTest.java
```

## Architecture

Layered monolith with explicit responsibilities:
- **Controller**: HTTP contract, request parsing/validation.
- **Service**: business rules, transaction boundaries, orchestration.
- **Repository**: SQL execution and result mapping.
- **Domain**: core immutable model and invariants.
- **Config**: infrastructure and tunable properties.

This separation keeps the monolith easy to evolve into services later.

## Database Design

Migration file: `src/main/resources/db/migration/V1__initial_schema.sql`

### Tables

#### `users`
- `id` PK
- `name`, `age`
- `attributes_json` for optional profile attributes
- `created_at`
- `is_fraud`, `fraud_marked_at`

#### `profile_visits`
- `id` PK
- `visitor_user_id` FK -> `users.id`
- `visited_user_id` FK -> `users.id`
- `created_at`
- check constraint to prevent self-visit

#### `profile_likes`
- `id` PK
- `liker_user_id` FK -> `users.id`
- `liked_user_id` FK -> `users.id`
- `created_at`
- unique (`liker_user_id`, `liked_user_id`) to prevent duplicate likes
- check constraint to prevent self-like

### Database Indexing Strategy

Indexes are intentionally added for high-frequency social-network lookups, where reads and interaction checks happen continuously under load.

Indexes exist for these interaction dimensions:
- `visitor_id` (implemented as `visitor_user_id`)
- `visited_id` (implemented as `visited_user_id`)
- `liker_id` (implemented as `liker_user_id`)
- `liked_id` (implemented as `liked_user_id`)

These indexes optimize:
- retrieving profile visitors quickly in reverse-chronological order
- checking duplicate likes efficiently via unique + lookup paths
- fraud detection queries that count distinct interactions inside time windows

Concrete indexes:
- `profile_visits(visited_user_id, created_at DESC, id DESC)`
  - optimized for visitor timeline queries by target user
- `profile_visits(visitor_user_id, created_at)`
  - optimized for fraud counting by actor + time range
- `profile_likes(liked_user_id, created_at DESC, id DESC)`
  - optimized for liked-by timeline style access
- `profile_likes(liker_user_id, created_at)`
  - optimized for fraud counting by actor + time range
- `users(created_at)`, `users(is_fraud)` for operational queries/flag scans

## Query Design: Retrieve Visitors of a User

```sql
SELECT pv.visitor_user_id, u.name, u.age, pv.created_at
FROM profile_visits pv
JOIN users u ON u.id = pv.visitor_user_id
WHERE pv.visited_user_id = ?
ORDER BY pv.created_at DESC, pv.id DESC
LIMIT ? OFFSET ?;
```

Why this performs well:
- `WHERE visited_user_id = ?` matches leading index column.
- `ORDER BY created_at DESC, id DESC` follows index ordering.
- `LIMIT/OFFSET` supports pagination and controls response size.
- Join on PK (`users.id`) is efficient.

## Database Migrations

This service uses Flyway for schema versioning and startup safety.

Why Flyway is used:
- keeps schema changes explicit and reviewable
- provides deterministic startup for every environment
- tracks applied versions in `flyway_schema_history`
- prevents accidental schema drift between developers/environments

How migrations run:
- Flyway starts automatically on application startup.
- It applies pending scripts from `classpath:db/migration` in version order.
- Existing applied migrations are not rerun.

How to add a new migration:
1. Create a new immutable SQL file with the next version number.
2. Follow naming format `V<version>__<description>.sql`.
3. Commit migration and deploy.

Example:
- `V2__add_index_to_visits.sql`

Migration safety rules:
- migrations are immutable once applied
- new changes are introduced only via new `V*` files
- avoid editing older migration files in shared environments

## Validation and Data Integrity

### Domain-level (`UserProfile`)
- non-empty name
- age range [13, 120]
- optional attributes map validated (count/key/value length)
- immutable object + defensive map copy

### Service-level
- actor and target IDs must exist
- cannot like self
- self-visit policy configurable
- fraud users blocked from creating new interactions

### Database-level
- FK constraints for referential integrity
- unique constraint for duplicate-like prevention
- check constraints for self-like / self-visit and valid name/age

## API Endpoints

### Create User

`POST /users`

Request:
```json
{
  "name": "alice",
  "age": 28,
  "attributes": {
    "city": "Berlin",
    "interests": "music"
  }
}
```

### Bulk Insert Users

`POST /users/bulk`

Request:
```json
{
  "users": [
    { "name": "u1", "age": 21, "attributes": {} },
    { "name": "u2", "age": 22, "attributes": { "lang": "en" } }
  ]
}
```

### Visit Profile

`POST /user/visit`

Request:
```json
{
  "visitorId": 1,
  "visitedId": 2
}
```

### Like Profile

`POST /user/like`

Request:
```json
{
  "likerId": 1,
  "likedId": 2
}
```

### Get Visitors

`GET /user/visitors?userId=2&limit=100&offset=0`

## Fraud Detection Design

Rule: if a user **visits and likes at least 100 distinct users** in the first 10 minutes after account creation, mark as fraud.

Current implementation:
1. Each visit/like write succeeds first.
2. Service runs a bounded time-window count query for distinct visited and liked targets.
3. If both counts reach threshold, mark user as fraud in `users`.
4. Future interactions from fraud users are rejected.

Tradeoffs:
- **Pros**: simple, deterministic, no extra infrastructure.
- **Cons**: extra read queries per interaction at high write rates.

Scalable alternatives:
- async event stream + stateful fraud worker (windowed aggregation)
- Redis sorted sets/hyperloglog for near-real-time counters
- scheduled materialized rollups for lower write-path latency

## Bulk Insertion Strategy

`UserService.bulkInsert` uses JDBC batching:
- prepared statement reuse
- configurable batch chunking (`BATCH_SIZE = 500`)
- single transaction per API call
- MySQL optimization: `rewriteBatchedStatements=true` in JDBC URL

This reduces network round trips and improves insert throughput significantly.

## Testing Strategy

Tests are focused and fast:
- `FraudDetectionServiceTest`
  - flagged at threshold
  - not flagged below threshold
  - time window edge behavior
- `InteractionServiceTest`
  - self-like blocked
  - duplicate like blocked
  - missing user validation
  - successful interaction triggers fraud evaluation
- `UserProfileTest`
  - domain invariants and immutability checks

## Running Tests

Run tests before starting the app to catch regressions early.

1. Run all tests:
   ```bash
   ./mvnw test
   ```
   On Windows PowerShell:
   ```powershell
   .\mvnw.cmd test
   ```
2. Optional full verification (includes test lifecycle checks):
   ```bash
   ./mvnw clean verify
   ```
3. Typical local flow:
   - start database with Docker
   - run tests
   - start the application

## How to Run

1. Start database:
   ```bash
   docker compose up -d
   ```
2. Start app:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Optional API check:
   - `http://localhost:8080/actuator/health`

Flyway migrations run automatically at startup.

## Scalability Notes

- Read/write separation can be added later (replica reads for visitors API).
- Partition interaction tables by time or user hash for very large datasets.
- Add idempotency keys for client retries.
- Add rate limiting at gateway/service boundary.
- Move fraud checks async when interaction throughput grows.

## Microservices Evolution Plan

Possible decomposition:

### 1) User Service
- owns `users` + profile metadata
- APIs: create/get/update user, user status (fraud state)

### 2) Interaction Service
- owns visits/likes writes and reads
- APIs: `/visit`, `/like`, `/visitors`

### 3) Fraud Service
- consumes interaction events
- computes sliding-window/threshold logic
- emits fraud decision events

### Communication Pattern
- synchronous REST for query-style requests
- asynchronous events (Kafka/RabbitMQ) for state propagation:
  - `interaction.recorded`
  - `fraud.user_flagged`
  - optional `fraud.user_unflagged`

### How Interaction Service learns fraud decisions

Realistic event-driven flow:
1. Interaction Service publishes `interaction.recorded` events.
2. Fraud Service consumes events and updates user risk window state.
3. On threshold hit, Fraud Service publishes `fraud.user_flagged`.
4. Interaction Service consumes `fraud.user_flagged` and updates local projection/cache (`blocked_users`).
5. On new `/visit` or `/like`, Interaction Service checks local projection before write; reject if blocked.

Benefits:
- decoupled ownership
- low-latency write path
- eventual consistency with clear event contracts

Consistency note:
- very short race windows are possible; mitigate via:
  - synchronous fallback check to User/Fraud Service on suspicious actors
  - compensating actions (revert interaction) if a late fraud decision arrives

### API Versioning & Resilience
- version endpoints (`/api/v1/...`) to evolve contracts safely.
- use circuit breakers/timeouts/retries for sync calls.
- prefer idempotent consumers and at-least-once delivery handling for events.
