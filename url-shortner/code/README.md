# URL Shortener -- Spring Boot 4 + Cassandra + Kafka

Implementation of the design in [`../design/url-shortner-design.md`](../design/url-shortner-design.md).

## Stack

- Java 21 / Spring Boot 4.0.6 / Spring WebFlux (reactive)
- PostgreSQL + Liquibase (`url_mapping`, `token_range`)
- Cassandra reactive (`click_events`, `click_counts` counter table)
- Kafka KRaft (`click-events` topic, fire-and-forget producer + `@KafkaListener` consumer)
- Redis (Lettuce reactive) for `url:{shortCode}` cache, 60-min TTL

## Run

```bash
mvn package -DskipTests
docker compose up -d --build
# wait until health checks pass
curl http://localhost:8090/v3/api-docs > /dev/null && echo ready
```

## API

```bash
# 1. Shorten
curl -X POST http://localhost:8090/api/urls \
  -H 'Content-Type: application/json' \
  -d '{"longUrl":"https://www.google.com"}'
# => {"shortCode":"mO","shortUrl":"http://localhost:8090/mO", ...}

# 2. Redirect (302)
curl -I http://localhost:8090/mO

# 3. Inspect metadata
curl http://localhost:8090/api/urls/mO

# 4. Stats (read from Cassandra click_counts)
curl http://localhost:8090/api/urls/mO/stats
# => {"shortCode":"mO","totalClicks":8,"dailyCounts":[{"day":"2026-04-26","count":8}]}
```

Swagger UI: <http://localhost:8090/swagger-ui.html>.

## Notable design points

- **Token allocator** (`TokenRangeAllocator`): fetches batches of 1000 IDs from the
  PostgreSQL `token_range` row via atomic `UPDATE ... RETURNING`. One DB write
  per 1000 shortens. Concurrent `next()` callers coalesce on a shared
  in-flight `Mono` so only one DB round-trip happens per refresh.
- **Persistable**: `UrlMapping implements Persistable<String>` because the
  short_code PK is manually assigned -- without `isNew()` Spring Data R2DBC
  treats `save()` as UPDATE and silently inserts nothing.
- **Counter table**: Spring Data Cassandra can't model COUNTER columns from
  annotations, so `click_counts` is created by `ClickCountsSchemaInitializer`
  on startup and updated via raw CQL in the consumer.
- **Fire-and-forget click publish**: redirect latency depends only on
  Redis + PG, never on Kafka health.
- **Liquibase + R2DBC**: needs an explicit JDBC `DataSource` bean
  (`LiquibaseConfig`) because Spring Boot 4's Liquibase autoconfig only fires
  when a `DataSource` exists, and pure R2DBC services don't have one.

## Inspect storage

```bash
docker exec shortener-postgres-1  psql -U shortener -d shortener -c \
  "SELECT short_code, left(long_url,60), expires_at::date FROM url_mapping;"

docker exec shortener-cassandra-1 cqlsh -e \
  "SELECT * FROM url_analytics.click_counts;"

docker exec shortener-redis-1 redis-cli KEYS 'url:*'
```

## Teardown

```bash
docker compose down -v
```
