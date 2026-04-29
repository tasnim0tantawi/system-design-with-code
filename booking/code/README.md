# Online Hotel Booking System (POC)

Reactive, microservices-based proof of concept derived from the Phase 2 design
in [`../design/booking-design.md`](../design/booking-design.md). Each domain
service owns its own Postgres database, registers with Eureka, and is fronted
by a Spring Cloud Gateway that validates JWTs.

## Tech stack

- Java 21
- Spring Boot 4.0.6
- Spring Cloud 2025.1.1
- Spring WebFlux (reactive, non-blocking)
- Spring Data R2DBC + `org.postgresql:r2dbc-postgresql` (PostgreSQL primary stores)
- Spring Data Cassandra Reactive (booking history, append-only)
- Spring Data Redis Reactive (Lettuce) -- cache + JWT blocklist + refresh tokens
- Apache Kafka (KRaft mode, `apache/kafka-native:3.9.0`) + Spring Kafka 4.0.0
- MeiliSearch v1.12 (full-text hotel search index)
- Liquibase (PostgreSQL schema + seed migrations)
- Spring Cloud Netflix Eureka (server + client)
- Spring Cloud Gateway (reactive, `spring-cloud-starter-gateway-server-webflux`)
- JJWT 0.12.6
- Maven multi-module

## Modules and ports

| Module                | Port  | Purpose |
|-----------------------|-------|---------|
| `common`              |  -    | Shared JAR: JWT util + token keys, `LiquibaseConfig`, exceptions, ProblemDetail handler, cursor pagination, security helpers |
| `eureka-server`       | 8761  | Service discovery |
| `api-gateway`         | 8080  | Single ingress; validates JWT signature, checks Redis blocklist (logout), forwards `X-User-Id` / `X-User-Role` |
| `auth-service`        | 8081  | Register users + managers, issue access + refresh token pairs, refresh, logout |
| `hotel-service`       | 8082  | Hotels, rooms, availability; publishes `hotel.*` Kafka events for the search index |
| `booking-service`     | 8083  | Pending → confirmed → cancelled lifecycle, mocked payments, writes append-only history to Cassandra, publishes `booking.*` to Kafka |
| `search-service`      | 8084  | Consumes `hotel.*` from Kafka, projects into MeiliSearch; serves cached search queries |
| `review-service`      | 8085  | Reviews + synchronous aggregates, publishes `review.*` to Kafka |
| `notification-service`| 8086  | Consumes `booking.*` and `review.*`, persists in-app notifications |

Single Postgres (5 databases) + Redis + Cassandra + Kafka + MeiliSearch, all shared.
Container names are prefixed `booking-` (the compose project name).

## Run

```bash
docker compose up --build -d
```

Wait ~30s after compose comes up for all services to register with Eureka.
Visit `http://localhost:8761` to confirm registrations.

The gateway is the only externally relevant port (`http://localhost:8080`),
but every service also exposes its own port for direct debugging.

## Seeded login credentials

On first startup Liquibase seeds users (passwords match the role):

| Email | Role | Password |
|---|---|---|
| `manager1@hotels.com`, `manager2@hotels.com`, `manager3@hotels.com` | MANAGER | `manager123` |
| `tasnim@guests.com`, `amira@guests.com`, `mariam@guests.com`, `hajar@guests.com` | USER | `user123` |
| `admin@booking.com` | ADMIN | `admin123` |

6 hotels, 14 rooms, 365 days of availability, 7 bookings, 5 reviews and 7 notifications
are seeded as well; Cassandra `booking_history` is populated by `CassandraSeedRunner`
on startup.

## Sample curl flow

```bash
# 1. Login as a seeded user (or POST /api/auth/register/{user|manager} for a new account)
LOGIN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"tasnim@guests.com","password":"user123"}')
ACCESS=$(echo "$LOGIN"  | jq -r .accessToken)
REFRESH=$(echo "$LOGIN" | jq -r .refreshToken)
USER_ID=$(echo "$LOGIN" | jq -r .userId)

# 2. Manager creates a hotel + room (login as manager1@hotels.com first)
MGR=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"manager1@hotels.com","password":"manager123"}' | jq -r .accessToken)

HOTEL=$(curl -s -X POST http://localhost:8080/api/hotels \
  -H "Authorization: Bearer $MGR" -H 'Content-Type: application/json' \
  -d '{"name":"Sea View","description":"nice","location":"Lisbon",
       "stars":4,"type":"hotel","businessCredentials":"BIZ-1"}')
HOTEL_ID=$(echo "$HOTEL" | jq -r .id)

curl -s -X POST http://localhost:8080/api/hotels/$HOTEL_ID/rooms \
  -H "Authorization: Bearer $MGR" -H 'Content-Type: application/json' \
  -d '{"roomType":"double","basePrice":100.00,"totalCount":3}'

# 3. Search via MeiliSearch (no auth required for GET hotels/search)
curl -s "http://localhost:8080/api/hotels/search?location=Lisbon"

# 4. List hotels with cursor pagination (also public)
curl -s "http://localhost:8080/api/hotels?limit=3"

# 5. User creates a booking
BOOKING=$(curl -s -X POST http://localhost:8080/api/hotels/$HOTEL_ID/bookings \
  -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' \
  -d "{\"roomId\":1,\"checkIn\":\"2026-06-01\",\"checkOut\":\"2026-06-03\"}")
BOOKING_ID=$(echo "$BOOKING" | jq -r .id)

# 6. Pay -- writes Cassandra history + publishes booking.confirmed to Kafka
curl -s -X POST http://localhost:8080/api/bookings/$BOOKING_ID/payment \
  -H "Authorization: Bearer $ACCESS"

# 7. Notification was created via Kafka consumer -- check the inbox
curl -s "http://localhost:8080/api/users/$USER_ID/notifications" \
  -H "Authorization: Bearer $ACCESS"

# 8. Booking history (Cassandra, partitioned by userId)
curl -s "http://localhost:8080/api/users/$USER_ID/booking-history" \
  -H "Authorization: Bearer $ACCESS"

# 9. Refresh tokens (rotation: old refresh is consumed)
curl -s -X POST http://localhost:8080/api/auth/refresh \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}"

# 10. Logout -- access JTI added to Redis blocklist, refresh token deleted
curl -s -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $ACCESS" \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}"
```

## What was added on top of the original POC

The README originally listed several "Phase 2 simplifications". These have now
been replaced with real implementations:

1. **Kafka (KRaft, no ZooKeeper)** -- replaces the previous synchronous
   `WebClient` call from `booking-service` / `review-service` to
   `notification-service`. Topics:
   - `booking-events` -- produced by booking-service (`booking.confirmed`,
     `booking.cancelled`), consumed by notification-service.
   - `review-events` -- produced by review-service, consumed by notification-service.
   - `hotel-events` -- produced by hotel-service (`hotel.created`,
     `hotel.updated`, `hotel.deleted`), consumed by search-service for indexing.
   - `click-events` (URL shortener project; documented separately).

   Spring Boot 4 has **no Kafka autoconfiguration**, so each service has a
   manually-defined `KafkaProducerConfig` / `KafkaConsumerConfig` that builds
   `KafkaTemplate` and `ConcurrentKafkaListenerContainerFactory` beans.

2. **Cassandra for booking history** -- `booking-service` writes one row per
   confirmed booking to `booking_history.booking_history`, partitioned by
   `userId` and clustered by `(createdAt DESC, bookingId)`. Read endpoint
   `GET /api/users/{userId}/booking-history` returns a single-partition scan.
   Schema is created via `SchemaAction.CREATE_IF_NOT_EXISTS` plus a custom
   keyspace creation. Sample rows are inserted at startup by `CassandraSeedRunner`.

3. **MeiliSearch (replaces Elasticsearch)** -- chosen over Elasticsearch
   because the operational profile is much smaller for this workload: a single
   binary, REST API, typo tolerance + facets out of the box, ~50 MB image vs ~1 GB.
   `search-service`:
   - Hosts a `HotelIndexer` `@KafkaListener` that upserts/deletes documents on
     hotel events.
   - On startup, configures filterable + searchable attributes via the settings API.
   - Serves search via Redis-cached calls to MeiliSearch's `POST /indexes/hotels/search`.

4. **Liquibase** -- replaces the previous `spring.sql.init` schema bootstrap. Each
   service has `db/changelog/db.changelog-master.yaml` that includes
   `V001__initial_schema.sql` (DDL) + `V002__seed_*.sql` (data). Boot 4's R2DBC
   has no `DataSource` so the autoconfig never fires; `common/LiquibaseConfig`
   builds a `SpringLiquibase` from `spring.liquibase.url` + JDBC driver.

5. **Refresh tokens + Redis logout** -- access tokens are short-lived JWTs
   (15 min) carrying a `jti`; refresh tokens are opaque random 32-byte strings
   stored at `refresh:{token}` in Redis (TTL 7 d). `/refresh` rotates via
   atomic `GETDEL`. `/logout` writes `blocklist:{jti}` with TTL = remaining
   access-token lifetime; the gateway checks this blocklist before forwarding.

6. **Jakarta Validation + ProblemDetail** -- request DTOs use `@NotBlank`,
   `@Email`, `@Min/@Max`, etc. Validation failures and all other exceptions
   come back as RFC 7807 `application/problem+json` with `code`, `timestamp`,
   and (for validation) a per-field `errors` array.

7. **Public hotel browsing** -- the gateway lets anonymous `GET /api/hotels/**`
   through (except paths containing `/bookings`, which stay manager-only) so
   the frontend can render the catalog without forcing login.

8. **Cursor-based pagination** -- `GET /api/hotels?cursor=...&limit=...`
   returns `{items, nextCursor, hasMore}` using the `WHERE id > :cursor LIMIT
   :n+1` pattern. Cursor is opaque Base64.

## Remaining POC simplifications

A few intentional shortcuts remain:

- **Synchronous review aggregates** -- design says async via a worker; the
  aggregate row is still upserted in the same request via SQL `ON CONFLICT`.
- **Review eligibility** -- accepts both `confirmed` and `completed` bookings
  so the curl flow works without simulating check-in.
- **Password hashing** -- SHA-256 with a fixed salt; production must use BCrypt/Argon2.
- **Per-room availability** -- seeded for 365 days at room creation so the
  `/availability` endpoint always has data; production would use a scheduled
  backfill plus on-demand row creation.
- **Single Kafka partition per topic** -- fine for development; for >100K
  msgs/s split into N partitions keyed by hotel/booking/user ID.
- **Click events fire-and-forget** -- a Kafka outage drops events silently;
  audit-grade analytics would use the transactional outbox pattern.

## Mock external interfaces

Every external dependency is an interface with a `Mock*Impl` annotated
`@ConditionalOnProperty(name="external.mocks.enabled", havingValue="true",
matchIfMissing=true)`. Wired in by default; to swap one out, set
`external.mocks.enabled=false` and provide your own `@Component` implementation.

| Service              | Interface                | Mock behavior |
|----------------------|--------------------------|---------------|
| hotel-service        | `HotelVerificationApi`   | returns `true` after 100ms |
| hotel-service        | `ObjectStorage`          | returns `https://cdn.example.com/{key}` and a fake PUT URL |
| booking-service      | `PaymentGateway`         | succeeds unless amount ends in `.99` |
| review-service       | `ObjectStorage`          | same shape as hotel's |
| notification-service | `EmailSmsProvider`       | logs and returns success |

## Architecture overview

External traffic enters via the API Gateway, which authenticates the JWT
(signature + Redis blocklist) and forwards trusted user-identity headers; each
domain service owns its own database; services communicate via Kafka for
async fan-out (booking → notifications, hotel → search index) and via
`WebClient` over Eureka load-balancing for synchronous calls (booking
decrementing room availability).

For full design rationale, ERDs, and per-service decisions see
[`../design/booking-design.md`](../design/booking-design.md). The design folder
also contains rendered architecture diagrams under `design/diagrams/`.
