from fpdf import FPDF
import textwrap

QA = [
    # -- SYSTEM DESIGN & ARCHITECTURE ------------------------------------------
    {
        "section": "System Design & Architecture",
        "q": "Walk me through the overall architecture of this hotel booking system.",
        "a": (
            "The system is a microservices architecture composed of 8 backend services plus an API gateway. "
            "Each service owns its domain: auth-service manages identity, hotel-service manages hotel/room data, "
            "booking-service handles reservations, search-service provides full-text hotel search, "
            "review-service manages guest reviews, notification-service delivers in-app notifications, "
            "and eureka-server provides service discovery. The API gateway is the single entry point -- "
            "it validates JWTs, strips the Authorization header, and forwards X-User-Id / X-User-Role headers "
            "to downstream services. All services are reactive (Spring WebFlux), communicate over HTTP via Eureka "
            "load-balancing, and publish/consume domain events through Kafka for async decoupling."
        ),
    },
    {
        "section": "System Design & Architecture",
        "q": "Why did you choose microservices over a monolith for this project?",
        "a": (
            "Each domain has a distinct scaling profile: search needs to scale independently of booking, "
            "notifications can be eventually consistent while bookings need strong consistency. "
            "Microservices allow independent deployment, technology choice per service (Cassandra for append-only "
            "booking history, Postgres for transactional data, MeiliSearch for full-text), and fault isolation -- "
            "a slow review-service doesn't degrade the booking flow. The trade-off is operational complexity "
            "(service discovery, distributed tracing, network failures), which we accept because the domain "
            "decomposition is natural and each team could own one service."
        ),
    },
    {
        "section": "System Design & Architecture",
        "q": "How does the API Gateway work and what responsibilities does it have?",
        "a": (
            "The API Gateway (Spring Cloud Gateway on WebFlux) has three responsibilities: "
            "1) JWT authentication -- a WebFilter validates the Bearer token on every request except /api/auth/**; "
            "2) Routing -- YAML-defined routes match URL predicates and load-balance via Eureka (lb://service-name); "
            "3) Header propagation -- on successful auth, the filter removes the Authorization header and injects "
            "X-User-Id and X-User-Role so downstream services trust the gateway and never re-parse JWTs. "
            "Route order matters: /api/hotels/search/** is declared before /api/hotels/** so the more-specific "
            "search route wins the predicate match."
        ),
    },
    {
        "section": "System Design & Architecture",
        "q": "Why is the search route declared before the hotel route in the gateway config?",
        "a": (
            "Spring Cloud Gateway evaluates routes in declaration order and stops at the first predicate match. "
            "The hotel route uses Path=/api/hotels/**, which is a wildcard that also matches /api/hotels/search/**. "
            "If hotel came first, search requests would be incorrectly routed to hotel-service. "
            "Declaring search-service's route earlier ensures /api/hotels/search/** wins before the broader "
            "/api/hotels/** catch-all. The config even has a comment: 'IMPORTANT: search route must be declared "
            "BEFORE /api/hotels/** to win predicate match'."
        ),
    },
    {
        "section": "System Design & Architecture",
        "q": "How do services communicate with each other?",
        "a": (
            "Two patterns are used. Synchronous: services that need an immediate response use Spring WebClient "
            "with Eureka service discovery (lb:// URI scheme). The load balancer resolves the service name to "
            "a healthy instance. Asynchronous: for cross-service events that don't need a response "
            "(booking confirmed -> notification, hotel created -> search index), we use Kafka. "
            "The producer fires-and-forgets (with onErrorResume so a Kafka outage doesn't fail the user request). "
            "Consumers process at their own pace, providing natural backpressure and decoupling."
        ),
    },
    {
        "section": "System Design & Architecture",
        "q": "What is the role of Eureka in this system?",
        "a": (
            "Eureka is the service registry. On startup each service registers itself with its hostname and port. "
            "When service A needs to call service B, it asks the Eureka client for B's instances -- the client "
            "caches the registry locally and refreshes every 30 seconds. The Spring Cloud LoadBalancer picks "
            "an instance (round-robin by default). This removes hardcoded URLs from config. In Docker, each "
            "service sets EUREKA_CLIENT_SERVICEURL_DEFAULTZONE to point at the eureka-server container."
        ),
    },
    {
        "section": "System Design & Architecture",
        "q": "What databases does each service use and why?",
        "a": (
            "auth-service: PostgreSQL -- relational, ACID, perfect for user credentials and role data. "
            "hotel-service: PostgreSQL -- hotels, rooms, and availability are relational with FK constraints. "
            "booking-service: PostgreSQL for active bookings (transactional), Cassandra for booking history "
            "(append-only, user-partitioned, high-read throughput). "
            "search-service: MeiliSearch for full-text search, Redis for query result caching. "
            "review-service: PostgreSQL -- reviews are relational with hotel/user FKs. "
            "notification-service: PostgreSQL -- notifications per user, simple CRUD. "
            "Redis is used across hotel-service (hotel cache) and all search results (60 s TTL)."
        ),
    },
    {
        "section": "System Design & Architecture",
        "q": "How would you handle distributed transactions across services (e.g., booking + inventory decrement)?",
        "a": (
            "We use the Saga pattern -- a sequence of local transactions coordinated by events. "
            "When a booking is paid: booking-service updates its own DB to CONFIRMED, then calls hotel-service "
            "synchronously to decrement room availability (an internal HTTP call). If that fails, booking-service "
            "rolls back to PENDING. There is no two-phase commit -- instead, each step has a compensating transaction. "
            "In a production system we'd use Kafka to orchestrate the saga: booking emits booking.confirmed, "
            "hotel-service consumes it and decrements availability, then emits inventory.decremented. "
            "If decrement fails, it emits inventory.failed and booking-service compensates by reverting the booking."
        ),
    },
    {
        "section": "System Design & Architecture",
        "q": "How does the system prevent double-booking?",
        "a": (
            "Room availability is tracked per-date in a room_availability table with an available_count column. "
            "The decrement operation uses an atomic SQL UPDATE with a WHERE available_count > 0 clause -- "
            "if the row is already 0, the update affects 0 rows and we reject the booking. "
            "In a high-concurrency scenario we'd also add a Redis distributed lock keyed on "
            "'lock:room:{roomId}:{date}' with a short TTL (e.g., 5 seconds) around the check-and-decrement "
            "to prevent race conditions between the check and the SQL update."
        ),
    },
    {
        "section": "System Design & Architecture",
        "q": "How would you add rate limiting to the API gateway?",
        "a": (
            "Spring Cloud Gateway has a built-in RequestRateLimiter filter backed by Redis. "
            "You configure it per-route with a replenish rate (tokens/second) and burst capacity. "
            "The filter uses the token bucket algorithm implemented atomically in Redis via a Lua script, "
            "so it's distributed across gateway replicas. You'd add: "
            "filters: - name: RequestRateLimiter args: redis-rate-limiter.replenishRate: 10 "
            "redis-rate-limiter.burstCapacity: 20 to protected routes, with a KeyResolver "
            "that extracts the X-User-Id header so limits are per-user rather than per-IP."
        ),
    },

    # -- REACTIVE PROGRAMMING & WEBFLUX ----------------------------------------
    {
        "section": "Reactive Programming & WebFlux",
        "q": "Why did you choose Spring WebFlux over Spring MVC for all services?",
        "a": (
            "All I/O in this system is network-bound: database queries (R2DBC, Redis, Cassandra reactive), "
            "Kafka sends, and inter-service HTTP calls. WebFlux uses a small fixed thread pool (Netty event loop) "
            "and suspends I/O-waiting tasks without blocking threads, enabling high concurrency with low memory. "
            "Spring MVC uses one thread per request -- under heavy load it exhausts the thread pool. "
            "The trade-off: reactive code is harder to read, debug, and test; stack traces are fragmented. "
            "We accept that cost because the I/O-heavy workload justifies it."
        ),
    },
    {
        "section": "Reactive Programming & WebFlux",
        "q": "What is the difference between Mono and Flux?",
        "a": (
            "Both are reactive types from Project Reactor. Mono<T> represents a stream of 0 or 1 element -- "
            "used for operations that return a single value or nothing (e.g., findById, save). "
            "Flux<T> represents a stream of 0 to N elements -- used for collections or infinite streams "
            "(e.g., findAll, streaming events). Both are lazy: nothing executes until someone subscribes. "
            "Operators like map, flatMap, filter, collectList transform the stream without blocking. "
            "switchIfEmpty provides a fallback when the stream is empty -- used heavily for cache-miss logic."
        ),
    },
    {
        "section": "Reactive Programming & WebFlux",
        "q": "Explain how the Redis cache-aside pattern is implemented reactively.",
        "a": (
            "In HotelService.getHotel(): redis.opsForValue().get(cacheKey) returns a Mono<String>. "
            "If the key exists, flatMap deserializes and returns the cached response. "
            ".switchIfEmpty(...) triggers only when the Mono is empty (cache miss) -- it queries Postgres, "
            "serializes the result, writes it to Redis with a 60 s TTL, then returns the value. "
            "The chain is fully non-blocking: no thread waits; the event loop is released between each async step "
            "and resumes via callbacks."
        ),
    },
    {
        "section": "Reactive Programming & WebFlux",
        "q": "What happens when you call .subscribe() inside a @KafkaListener method?",
        "a": (
            "The @KafkaListener runs on a Kafka consumer thread (not a Netty event loop). Calling .subscribe() "
            "there schedules the reactive pipeline on the default Reactor scheduler and returns immediately, "
            "so the Kafka listener method completes and the consumer can poll the next message. "
            "This is intentional fire-and-forget: we don't block the consumer thread waiting for MeiliSearch "
            "or the notification DB write to finish. The risk is that errors in the pipeline are only logged, "
            "not propagated -- in production you'd add dead-letter topic handling."
        ),
    },
    {
        "section": "Reactive Programming & WebFlux",
        "q": "Why can't you use a traditional synchronized block or ThreadLocal in a reactive service?",
        "a": (
            "In reactive code, a single logical operation is executed across multiple threads as it moves through "
            "the event loop. A ThreadLocal set in one operator callback may not be visible in the next because "
            "they may run on different threads. synchronized blocks block the current thread, which defeats the "
            "purpose of reactive I/O -- you'd starve the event loop. Instead, use Reactor's Context for "
            "request-scoped data propagation, and design state to flow through the reactive pipeline as "
            "immutable values rather than shared mutable state."
        ),
    },
    {
        "section": "Reactive Programming & WebFlux",
        "q": "How does R2DBC differ from JDBC and why is it used here?",
        "a": (
            "JDBC is blocking -- each database call blocks the calling thread until the response arrives. "
            "R2DBC (Reactive Relational Database Connectivity) is the non-blocking equivalent: it returns "
            "Publisher (Mono/Flux) types so the event loop thread is released while waiting for the DB. "
            "Spring Data R2DBC provides reactive repository interfaces (ReactiveCrudRepository) that generate "
            "SQL and return reactive types. The trade-off: R2DBC lacks some JDBC features like stored procedure "
            "support and has a smaller ecosystem; but for an I/O-bound microservice the throughput gains justify it."
        ),
    },
    {
        "section": "Reactive Programming & WebFlux",
        "q": "What is flatMap vs map in a reactive stream and when do you use each?",
        "a": (
            "map transforms each element synchronously: T -> R. Use it for pure in-memory transformations "
            "like entity-to-DTO conversion (e.g., .map(this::toResponse)). "
            "flatMap transforms each element into a new Publisher (Mono/Flux) and subscribes to it, "
            "merging results: T -> Mono<R>. Use it when the transformation itself involves async I/O, "
            "like calling a repository or external API. Incorrect use of map for async operations "
            "returns Mono<Mono<R>> (a wrapped publisher) instead of Mono<R> -- a common bug."
        ),
    },
    {
        "section": "Reactive Programming & WebFlux",
        "q": "How do you handle errors in reactive streams in this project?",
        "a": (
            "Several patterns are used. onErrorResume: used in Kafka producers to swallow send failures "
            "silently (fire-and-forget) -- a Kafka outage doesn't fail the HTTP response. "
            "switchIfEmpty + Mono.error: used in services to return 404 ApiException when a repository "
            "returns empty (e.g., hotelRepo.findById(id).switchIfEmpty(Mono.error(ApiException.notFound(...)))). "
            "Global error handler: @ControllerAdvice / @ExceptionHandler on ApiException maps to HTTP status codes. "
            "In production you'd also add retries with exponential backoff for transient failures."
        ),
    },
    {
        "section": "Reactive Programming & WebFlux",
        "q": "How is backpressure handled in this system?",
        "a": (
            "Reactor implements the Reactive Streams specification which mandates backpressure support. "
            "When a Flux produces items faster than the subscriber can process, the subscriber signals demand "
            "via request(n) and the publisher only emits n items at a time. In practice: the WebFlux HTTP layer "
            "requests chunks from the reactive pipeline aligned with network send-buffer availability. "
            "Kafka's ConcurrentKafkaListenerContainerFactory runs a fixed number of consumer threads "
            "providing implicit backpressure on consumption rate. For streaming large result sets from Postgres "
            "via R2DBC, the repository returns Flux<Entity> and R2DBC uses cursor-based fetching rather than "
            "loading all rows into memory."
        ),
    },

    # -- KAFKA ------------------------------------------------------------------
    {
        "section": "Apache Kafka",
        "q": "Why did you replace synchronous HTTP notification calls with Kafka?",
        "a": (
            "The original NotificationClient made blocking HTTP calls from booking-service to notification-service. "
            "Problems: 1) Temporal coupling -- if notification-service is down, the booking request fails. "
            "2) Latency -- the user waits for notification delivery before getting a booking confirmation. "
            "3) Retry complexity -- the caller must handle retries and timeouts. "
            "With Kafka, booking-service publishes an event and returns immediately. notification-service "
            "consumes at its own pace -- it can be down for minutes and catch up when it restarts. "
            "The trade-off is eventual consistency for notifications, which is acceptable."
        ),
    },
    {
        "section": "Apache Kafka",
        "q": "How is Kafka configured in this project? Why no ZooKeeper?",
        "a": (
            "Kafka runs in KRaft mode (Kafka Raft Metadata mode), which eliminates the ZooKeeper dependency "
            "introduced in Kafka 2.8 and made production-ready in 3.3. KRaft uses Kafka's own Raft consensus "
            "algorithm to manage cluster metadata, reducing operational complexity. The config sets "
            "process.roles=broker,controller on a single node, with a pre-generated CLUSTER_ID "
            "for storage format initialization. The native image (apache/kafka-native:3.9.0) was chosen "
            "for faster startup in containers."
        ),
    },
    {
        "section": "Apache Kafka",
        "q": "Spring Boot 4 has no Kafka autoconfiguration -- how did you solve that?",
        "a": (
            "Confirmed by inspecting the spring-boot-autoconfigure-4.0.6.jar AutoConfiguration.imports file -- "
            "zero Kafka entries. Spring Boot 4 restructured its autoconfiguration modules and Kafka "
            "autoconfiguration was not included in the initial release. Solution: manually create "
            "KafkaProducerConfig and KafkaConsumerConfig @Configuration classes that explicitly define "
            "ProducerFactory, KafkaTemplate, ConsumerFactory, and ConcurrentKafkaListenerContainerFactory beans "
            "-- the same beans that Boot 2/3 autoconfiguration would create. Add spring-kafka:4.0.0 to the "
            "parent BOM's dependencyManagement since the Boot 4 BOM also doesn't manage its version."
        ),
    },
    {
        "section": "Apache Kafka",
        "q": "What is a consumer group and how is it used here?",
        "a": (
            "A consumer group is a set of consumers that cooperate to consume a topic. Kafka distributes "
            "partitions across consumers in the group -- each partition is consumed by exactly one consumer "
            "at a time. This provides parallel consumption and fault tolerance. "
            "In this project: notification-service uses group 'notification-service' on both booking-events "
            "and review-events. search-service uses group 'search-service' on hotel-events. "
            "If multiple instances of search-service run (horizontal scaling), Kafka balances partitions "
            "among them. The group ID also determines the committed offset -- if search-service restarts, "
            "it resumes from the last committed offset, not from the beginning."
        ),
    },
    {
        "section": "Apache Kafka",
        "q": "What does AUTO_OFFSET_RESET=earliest mean and why is it set here?",
        "a": (
            "When a consumer group has no committed offset for a partition (first-time consumer or after "
            "a group reset), 'earliest' means start from the oldest available message. 'latest' means "
            "only consume messages produced after the consumer starts. "
            "We use 'earliest' so that if search-service starts after hotel-service has already published "
            "hotel.created events, it replays all of them and builds a complete index from scratch. "
            "This is important for the event-driven index: the MeiliSearch index is the projection of "
            "all hotel events, so replaying from offset 0 is how we rebuild it."
        ),
    },
    {
        "section": "Apache Kafka",
        "q": "How do you ensure message ordering in Kafka?",
        "a": (
            "Kafka guarantees ordering within a partition. Messages with the same key are always routed "
            "to the same partition (via key hashing). In this project, hotel events use the hotel ID as "
            "the key, booking events use the user ID. This ensures all events for a given hotel arrive "
            "in order at the search-service consumer. If ordering across hotels doesn't matter (it doesn't "
            "for independent index updates), multiple partitions can be used for throughput. "
            "With num.partitions=1 in our dev config, global ordering is guaranteed but throughput is limited."
        ),
    },
    {
        "section": "Apache Kafka",
        "q": "How would you handle a Kafka consumer failure / message reprocessing?",
        "a": (
            "The current implementation uses at-least-once delivery: Kafka commits offsets after the listener "
            "method returns. If the consumer crashes mid-processing, it will reprocess the message on restart. "
            "To handle this: 1) Make consumers idempotent -- MeiliSearch upsert is idempotent (same doc, same ID); "
            "2) Use a Dead Letter Topic (DLT) for messages that fail after N retries -- Spring Kafka's "
            "DefaultErrorHandler supports this; 3) For exactly-once semantics you'd use Kafka transactions "
            "(isolation.level=read_committed on the consumer), which is overkill for notification delivery "
            "but important for financial transactions."
        ),
    },
    {
        "section": "Apache Kafka",
        "q": "Why does Kafka produce to a topic asynchronously and what's the risk?",
        "a": (
            "Mono.fromFuture(kafka.send(topic, key, payload).toCompletableFuture()) wraps the async Kafka "
            "send in a reactive Mono. .onErrorResume swallows errors so a Kafka outage doesn't fail the "
            "HTTP response -- the booking confirmation reaches the user even if the event wasn't published. "
            "The risk: a silent data loss. If hotel-service publishes hotel.created but Kafka is down "
            "and swallows the error, the search index will be missing that hotel. "
            "Mitigations: use the transactional outbox pattern (write event to a DB table atomically with "
            "the hotel row, poll and publish via a separate process)."
        ),
    },

    # -- CASSANDRA -------------------------------------------------------------
    {
        "section": "Apache Cassandra",
        "q": "Why is Cassandra used for booking history specifically?",
        "a": (
            "Booking history is an append-only, write-heavy workload with high read throughput requirements. "
            "The access pattern is always 'get all bookings for user X', which maps perfectly to Cassandra's "
            "partition key model: partitioning by userId means all of a user's history is co-located on one "
            "node, making reads a single partition scan. Cassandra's LSM-tree storage excels at sequential "
            "writes. PostgreSQL would require an index scan across a large table. Cassandra also gives us "
            "tunable replication and no single point of failure -- appropriate for an audit/history store."
        ),
    },
    {
        "section": "Apache Cassandra",
        "q": "Explain the BookingHistoryKey composite primary key design.",
        "a": (
            "BookingHistoryKey has three components: userId (PARTITIONED) -- determines which node(s) hold "
            "the data; createdAt (CLUSTERED DESC) -- within a partition, rows are sorted by creation time "
            "descending so the most recent bookings come first without a secondary sort; "
            "bookingId (CLUSTERED) -- makes the key unique when a user has multiple bookings at the same "
            "timestamp. This design makes 'get history for user, newest first' a single partition read "
            "with no filtering. Adding createdAt to the clustering key also supports range queries "
            "('bookings in the last 30 days') efficiently."
        ),
    },
    {
        "section": "Apache Cassandra",
        "q": "What is a keyspace in Cassandra and what replication strategy did you choose?",
        "a": (
            "A keyspace is Cassandra's equivalent of a database schema -- it groups tables and defines "
            "replication strategy. We use SimpleStrategy with replication_factor=1, which is appropriate "
            "for a single-node development cluster. In production you'd use NetworkTopologyStrategy "
            "with a replication factor of 3 per data center, meaning each row is stored on 3 nodes. "
            "This provides fault tolerance: with RF=3 and consistency level QUORUM, the cluster tolerates "
            "1 node failure while still serving reads and writes."
        ),
    },
    {
        "section": "Apache Cassandra",
        "q": "How does Spring Data Cassandra Reactive work with AbstractReactiveCassandraConfiguration?",
        "a": (
            "Extending AbstractReactiveCassandraConfiguration triggers @EnableReactiveCassandraRepositories "
            "automatically (no separate annotation needed). The abstract class builds the CqlSession and "
            "ReactiveSession beans. You override getKeyspaceName(), getContactPoints(), getLocalDataCenter(), "
            "and getKeyspaceCreations() to provide connection details. getSchemaAction=CREATE_IF_NOT_EXISTS "
            "tells Spring to generate DDL for @Table entities if they don't exist. Interfaces extending "
            "ReactiveCassandraRepository get implementations generated at startup, with methods like "
            "findByKeyUserId returning Flux<BookingHistory>."
        ),
    },
    {
        "section": "Apache Cassandra",
        "q": "What is the local datacenter setting and why did it cause an issue?",
        "a": (
            "Cassandra 4.x defaults the datacenter name to 'datacenter1'. The CqlSession must be configured "
            "with the same datacenter name to connect. Initially the config had 'dc1' which didn't match, "
            "causing the error: 'some contact points are from a different DC: datacenter1'. "
            "The fix was to set spring.cassandra.local-datacenter=datacenter1 in application.yml and "
            "the same in docker-compose SPRING_CASSANDRA_LOCAL_DATACENTER=datacenter1. "
            "The local-datacenter setting is also used by the driver's load balancing policy to prefer "
            "nodes in the same DC for lower latency."
        ),
    },
    {
        "section": "Apache Cassandra",
        "q": "How does Cassandra handle consistency and availability differently from Postgres?",
        "a": (
            "Cassandra is AP in the CAP theorem -- it prioritizes availability and partition tolerance over "
            "strict consistency. PostgreSQL is CP -- it provides ACID transactions with strong consistency. "
            "In Cassandra, you choose consistency level per-operation: ALL (all replicas must respond), "
            "QUORUM (majority), ONE (fastest, weakest). There are no foreign keys, joins, or multi-row "
            "transactions. This makes Cassandra excellent for high-throughput writes and reads where "
            "eventual consistency is acceptable -- booking history is a perfect fit because we write once "
            "and read eventually; a few milliseconds of stale data is fine."
        ),
    },

    # -- MEILISEARCH & SEARCH --------------------------------------------------
    {
        "section": "MeiliSearch & Search",
        "q": "Why MeiliSearch instead of Elasticsearch for this project?",
        "a": (
            "MeiliSearch has a much simpler operational profile: no cluster management, no JVM tuning, "
            "no Lucene segment management. A single binary handles everything. Its REST API is developer-friendly "
            "and it provides typo-tolerance and full-text search out of the box. Elasticsearch is more "
            "powerful (aggregations, nested documents, complex mappings) but overkill for searching hotels "
            "by location name, stars, and type. MeiliSearch's Docker image is 50 MB vs Elasticsearch's 1 GB. "
            "For a hotel search use case with simple filters, MeiliSearch is the pragmatic choice."
        ),
    },
    {
        "section": "MeiliSearch & Search",
        "q": "How does the hotel search index stay in sync with the hotel database?",
        "a": (
            "Via an event-driven pipeline. hotel-service publishes hotel.created, hotel.updated, hotel.deleted "
            "events to the hotel-events Kafka topic after each write operation. search-service's HotelIndexer "
            "@KafkaListener consumes these events and calls MeiliSearchClient.upsert() or .delete() accordingly. "
            "MeiliSearch upsert is idempotent -- re-processing the same event (at-least-once delivery) just "
            "overwrites the document with identical data. The search index is eventually consistent with the "
            "hotel DB -- typically within seconds of the write."
        ),
    },
    {
        "section": "MeiliSearch & Search",
        "q": "What are filterable and searchable attributes in MeiliSearch and how are they configured?",
        "a": (
            "Searchable attributes determine which fields MeiliSearch includes in its inverted index for "
            "full-text search. We set name and location -- so searching 'Paris' finds hotels with 'Paris' "
            "in their name or location. Filterable attributes are indexed differently (as facets) for "
            "exact-match filtering: stars (numeric range), hotelType, status. "
            "These are configured via PATCH /indexes/hotels/settings at application startup in "
            "MeiliSearchClient.initIndex(). Without this, filter expressions throw a 'filterable attribute "
            "not configured' error from MeiliSearch."
        ),
    },
    {
        "section": "MeiliSearch & Search",
        "q": "How does the Redis cache work in front of MeiliSearch?",
        "a": (
            "SearchService builds a canonical cache key by concatenating all search parameters with '|' "
            "separators. On a request: redis.opsForValue().get(cacheKey) is tried first. "
            "If a cached JSON string is found, it's deserialized and returned immediately -- no MeiliSearch call. "
            "On a cache miss, MeiliSearch is queried, the result is serialized and stored in Redis with a "
            "60-second TTL, then returned. The TTL is short because hotel data changes infrequently but "
            "the index is updated asynchronously -- a 60 s stale window is acceptable. "
            "This reduces MeiliSearch load by orders of magnitude for popular repeated searches."
        ),
    },
    {
        "section": "MeiliSearch & Search",
        "q": "How would you handle the case where search-service starts before any hotels exist?",
        "a": (
            "The search index starts empty -- MeiliSearch returns an empty hits array, SearchService returns "
            "an empty SearchResponse. This is correct behavior. As hotels are created, hotel-service "
            "publishes events and HotelIndexer populates the index. "
            "If search-service has been down and missed events, on restart it reads from the committed offset "
            "(or offset 0 with AUTO_OFFSET_RESET=earliest for a fresh group) and replays all events, "
            "rebuilding the index. This is the power of Kafka as an event log -- the index is a "
            "deterministic projection of the event stream."
        ),
    },

    # -- REDIS -----------------------------------------------------------------
    {
        "section": "Redis & Caching",
        "q": "What caching strategy is used and why not write-through?",
        "a": (
            "Cache-aside (lazy loading). On read, check cache first; on miss, fetch from DB and populate cache. "
            "Writes invalidate the cache key (redis.delete('hotel:' + id)) rather than updating it. "
            "Write-through would update the cache on every write, keeping it always warm but adding "
            "latency to writes. Cache-aside is preferred here because: reads are far more frequent than "
            "writes for hotel data; invalidation on write is simple and correct; it avoids caching data "
            "that's never read (only accessed items are cached). The trade-off is a cache miss penalty "
            "on the first read after a write."
        ),
    },
    {
        "section": "Redis & Caching",
        "q": "How is Redis used for JWT token revocation in auth-service?",
        "a": (
            "JWTs are stateless by design -- the server doesn't track issued tokens. But for logout/revocation "
            "we need a way to invalidate them before expiry. auth-service stores revoked token JTIs (JWT IDs) "
            "in Redis with a TTL equal to the token's remaining lifetime. On each authenticated request, "
            "the gateway (or auth-service) checks if the token's JTI is in the Redis revocation set. "
            "If present, the request is rejected with 401. Since JTIs expire from Redis at the same time "
            "the token would expire naturally, the set stays bounded."
        ),
    },
    {
        "section": "Redis & Caching",
        "q": "What is ReactiveStringRedisTemplate and why use it over RedisTemplate?",
        "a": (
            "ReactiveStringRedisTemplate is the reactive equivalent of StringRedisTemplate. It returns "
            "Mono<String> / Flux<String> from operations rather than blocking. Using it in a WebFlux "
            "service is mandatory -- calling a blocking RedisTemplate from the Netty event loop thread "
            "would block that thread, starving all other requests on that event loop. "
            "ReactiveStringRedisTemplate uses Lettuce's reactive driver under the hood, which communicates "
            "with Redis over a non-blocking Netty channel."
        ),
    },
    {
        "section": "Redis & Caching",
        "q": "How would you handle cache stampede (thundering herd) on a cache miss?",
        "a": (
            "Cache stampede occurs when many concurrent requests all miss the cache simultaneously and all "
            "hit the database. Solutions: 1) Probabilistic early expiration -- refresh the cache slightly "
            "before TTL expires; 2) Request coalescing -- use a reactive Sinks or lock so only one request "
            "fetches from DB and the rest wait for the result; 3) Redis lock -- SET cache_lock:key 1 NX PX 5000 "
            "before querying DB; others wait and retry. For hotel data the TTL is 60 s and hotels are "
            "rarely updated so stampede is unlikely, but for high-traffic hotels we'd use a distributed lock."
        ),
    },

    # -- POSTGRESQL & R2DBC ----------------------------------------------------
    {
        "section": "PostgreSQL & R2DBC",
        "q": "How is the database schema initialized in each service?",
        "a": (
            "Each service has a src/main/resources/db/schema.sql file. Spring Boot's spring.sql.init.mode=always "
            "causes it to execute schema.sql on startup using the R2DBC connection. The SQL uses "
            "CREATE TABLE IF NOT EXISTS so it's idempotent -- running it on an already-initialized DB is safe. "
            "The postgres Docker service mounts a postgres/init.sql that creates the separate databases "
            "(auth, hotels, bookings, reviews, notifications) on first startup -- without this each service "
            "would connect to the default 'postgres' database."
        ),
    },
    {
        "section": "PostgreSQL & R2DBC",
        "q": "How do you handle database migrations in production vs the dev schema.sql approach?",
        "a": (
            "The schema.sql / spring.sql.init approach is a POC simplification. In production you'd use "
            "a migration tool like Flyway or Liquibase. Flyway stores a migration history table in the DB, "
            "runs only new migration scripts on startup, and supports rollback scripts. "
            "Each migration is versioned (V1__create_users.sql, V2__add_index.sql) and applied exactly once. "
            "This allows zero-downtime schema changes with backward-compatible migrations. Liquibase offers "
            "the same with XML/YAML changelogs and more rollback flexibility."
        ),
    },
    {
        "section": "PostgreSQL & R2DBC",
        "q": "How does dynamic pricing work for room availability?",
        "a": (
            "In HotelService.getAvailability(), a price multiplier is applied: if check-in is within 7 days "
            "of today, the base price is multiplied by 1.20 (20% surge pricing for last-minute bookings). "
            "Otherwise the multiplier is 1.0. The total price is pricePerNight * numberOfNights. "
            "The minimum available room count across all dates in the date range is calculated to determine "
            "the actual number of bookable rooms (the bottleneck date). This is a simple demand-based "
            "pricing model; production systems use ML models considering seasonality, events, and competitor pricing."
        ),
    },
    {
        "section": "PostgreSQL & R2DBC",
        "q": "What is the room_availability table and why seed 365 rows per room?",
        "a": (
            "room_availability stores one row per (room_id, date) with an available_count column. "
            "When a booking is made, the rows for the booked date range are decremented atomically. "
            "365 rows are seeded on room creation because the availability query needs rows to exist "
            "for every date in the check-in to check-out range -- a missing row means unknown availability, "
            "not zero. In production, seeding 365 rows per room is expensive for large hotels; instead "
            "you'd use a virtual availability model: store only exceptions (days when availability differs "
            "from total_count) and calculate available_count = total_count - booked_count on the fly."
        ),
    },

    # -- SECURITY & JWT --------------------------------------------------------
    {
        "section": "Security & JWT",
        "q": "How does JWT authentication work end-to-end in this system?",
        "a": (
            "1) User calls POST /api/auth/login with credentials. auth-service validates against DB, "
            "generates a JWT signed with HMAC-SHA256 using the shared JWT_SECRET, returns the token. "
            "2) Client includes Authorization: Bearer <token> on subsequent requests. "
            "3) The API Gateway's JwtAuthFilter intercepts the request, validates the signature and expiry, "
            "extracts userId and role from claims, then strips the Authorization header and adds "
            "X-User-Id and X-User-Role headers. "
            "4) Downstream services read these headers -- they trust the gateway, never re-validate the JWT. "
            "This centralizes auth logic and reduces per-service complexity."
        ),
    },
    {
        "section": "Security & JWT",
        "q": "What are the security risks of JWT and how would you mitigate them?",
        "a": (
            "1) Token theft -- if a JWT is stolen it's valid until expiry. Mitigate: short TTL (15 min access "
            "token + refresh token), HTTPS only, HttpOnly cookies instead of localStorage. "
            "2) Algorithm confusion -- accepting 'alg:none' or RS256/HS256 switching attacks. Mitigate: "
            "explicitly specify the expected algorithm in the verifier, reject others. "
            "3) Weak secret -- HS256 with a short secret is brute-forceable. Mitigate: 32+ byte random secret "
            "from a secrets manager (the dev config warns 'change-me-please-32bytes-min'). "
            "4) No revocation -- JWTs can't be invalidated before expiry. Mitigate: Redis revocation list as "
            "described in the auth-service design."
        ),
    },
    {
        "section": "Security & JWT",
        "q": "How is role-based access control (RBAC) implemented?",
        "a": (
            "Roles are embedded in the JWT claims as 'role': USER, MANAGER, or ADMIN. "
            "The gateway forwards X-User-Role to downstream services. Each service's controller extracts "
            "the role from the header and enforces permissions at the service layer. "
            "For example, hotel creation requires MANAGER role -- the service reads X-User-Role and rejects "
            "requests from non-managers. Hotel update also checks that the managerId on the Hotel entity "
            "matches the X-User-Id from the header, preventing one manager from editing another's hotel. "
            "In a more mature system you'd use Spring Security's method-level @PreAuthorize annotations."
        ),
    },
    {
        "section": "Security & JWT",
        "q": "Why is the JWT_SECRET passed as an environment variable and not hardcoded?",
        "a": (
            "Hardcoding secrets in source code means they're committed to version control and visible to "
            "anyone with repo access. Environment variables are injected at runtime from a secrets manager "
            "(AWS Secrets Manager, HashiCorp Vault, Kubernetes Secrets). The application.yml uses "
            "${JWT_SECRET:dev-secret-change-me-please-32bytes-min} -- the colon syntax provides a default "
            "for local development while requiring an explicit value in production. Docker Compose supports "
            "${JWT_SECRET:-default} syntax which reads from the host's environment. In production the "
            "secret would never appear in code or config files."
        ),
    },

    # -- DOCKER & DEPLOYMENT ---------------------------------------------------
    {
        "section": "Docker & Deployment",
        "q": "How is the Docker Compose depends_on with healthcheck used here?",
        "a": (
            "depends_on with condition: service_healthy makes a service wait until its dependency reports "
            "healthy before starting. Each infrastructure service has a healthcheck: Postgres uses "
            "pg_isready, Redis uses redis-cli ping, Kafka uses nc -z localhost 9092, Cassandra uses "
            "cqlsh -e 'describe cluster', MeiliSearch uses curl on /health. This prevents startup race "
            "conditions -- booking-service won't start until Cassandra and Kafka are fully ready. "
            "Without this, services start in parallel and connection errors during startup cause crashes "
            "that require manual restart."
        ),
    },
    {
        "section": "Docker & Deployment",
        "q": "Why does the Kafka native image require a mounted server.properties file?",
        "a": (
            "apache/kafka-native:3.9.0 uses a GraalVM native binary. The KafkaDockerWrapper that normally "
            "translates KAFKA_* environment variables to properties doesn't work correctly in the native "
            "image during the storage format step -- it throws 'advertised.listeners cannot use nonroutable "
            "address 0.0.0.0'. The fix: mount a server.properties with the correct configuration including "
            "advertised.listeners=PLAINTEXT://kafka:9092,CONTROLLER://kafka:9093 (using the container "
            "hostname, not 0.0.0.0). The CLUSTER_ID env var is still used for the format step ID."
        ),
    },
    {
        "section": "Docker & Deployment",
        "q": "How would you scale this system horizontally?",
        "a": (
            "Each service is stateless (state lives in external stores) so horizontal scaling is straightforward. "
            "Run multiple instances behind a load balancer -- Eureka distributes traffic automatically "
            "when multiple instances register. Kafka consumers scale by adding instances to the same "
            "consumer group -- Kafka rebalances partitions. Redis would move to Redis Cluster or Redis Sentinel. "
            "Postgres would use read replicas for read-heavy services and connection pooling (PgBouncer). "
            "Cassandra scales by adding nodes -- data rebalances automatically. The API gateway itself "
            "would scale behind an external load balancer (ALB, NGINX)."
        ),
    },
    {
        "section": "Docker & Deployment",
        "q": "What is a multi-stage Dockerfile and would you use one here?",
        "a": (
            "A multi-stage Dockerfile uses multiple FROM instructions. The first stage (build stage) uses "
            "a full JDK image to compile and package the JAR. The second stage (runtime stage) uses a "
            "minimal JRE image and copies only the JAR from the build stage. This reduces the final image "
            "size dramatically (JDK is ~400 MB, JRE distroless is ~100 MB) and reduces attack surface. "
            "For this project, each service's Dockerfile would: FROM maven:3.9-eclipse-temurin-21 AS build, "
            "run mvn package, then FROM eclipse-temurin:21-jre-alpine, COPY --from=build the JAR, CMD java -jar."
        ),
    },
    {
        "section": "Docker & Deployment",
        "q": "How would you implement zero-downtime deployments?",
        "a": (
            "1) Rolling deployment: bring up new instances before taking down old ones -- Kubernetes handles "
            "this natively with RollingUpdate strategy. 2) Blue-green deployment: run two identical "
            "environments, switch traffic from blue to green atomically via load balancer DNS update; "
            "instant rollback by switching back. 3) Database migrations must be backward-compatible: "
            "new code must work with old schema, old code must work with new schema -- add columns nullable "
            "first, populate data, then add constraints in a separate deployment. 4) API versioning so "
            "clients don't break when contracts change."
        ),
    },

    # -- API DESIGN ------------------------------------------------------------
    {
        "section": "API Design",
        "q": "How is OpenAPI/Swagger set up across all the microservices?",
        "a": (
            "Each service uses springdoc-openapi-starter-webflux-ui which auto-generates a /v3/api-docs "
            "endpoint from @RestController annotations and OpenAPI annotations (@Tag, @Operation). "
            "The API Gateway exposes a unified Swagger UI at /swagger-ui.html aggregating all services. "
            "Gateway routes proxy each service's /v3/api-docs to /v3/api-docs/{service} using RewritePath "
            "filters. The gateway's springdoc config lists all service URLs, so the Swagger UI shows a "
            "dropdown to switch between service specs. Controllers use @Tag(name='...') for grouping."
        ),
    },
    {
        "section": "API Design",
        "q": "What REST conventions does this API follow?",
        "a": (
            "Resource-based URLs with nested resources for ownership: /api/hotels/{hotelId}/bookings, "
            "/api/users/{userId}/reviews. HTTP methods are semantic: GET for reads, POST for create, "
            "PUT for full update. Response codes: 200 for success, 201 for creation, 400 for validation "
            "errors, 401 for unauthenticated, 403 for unauthorized, 404 for not found. "
            "Error responses use a consistent ApiException body with code, message, and timestamp fields. "
            "Pagination is not yet implemented (POC simplification) -- in production GET /api/hotels "
            "would support ?page=0&size=20 with a Page<HotelResponse> response."
        ),
    },
    {
        "section": "API Design",
        "q": "How would you version your APIs when breaking changes are needed?",
        "a": (
            "Three common approaches: 1) URL versioning -- /api/v1/hotels, /api/v2/hotels. Simple and "
            "cache-friendly but pollutes URLs. 2) Header versioning -- Accept: application/vnd.booking.v2+json. "
            "Cleaner URLs but harder to test in a browser. 3) Query param -- /api/hotels?version=2. "
            "For this system I'd use URL versioning at the gateway level -- add a v2 route that routes to "
            "a new service version or uses a transformer filter. Old and new versions run in parallel "
            "during migration. Deprecation is announced via headers (Deprecation, Sunset) before removal."
        ),
    },

    # -- SPRING BOOT & SPRING CLOUD --------------------------------------------
    {
        "section": "Spring Boot & Spring Cloud",
        "q": "What is Spring Cloud Gateway and how does it differ from Netflix Zuul?",
        "a": (
            "Spring Cloud Gateway (SCG) is built on Spring WebFlux and Netty -- fully non-blocking and reactive. "
            "Netflix Zuul 1.x is blocking (servlet-based); Zuul 2 is reactive but was never officially "
            "integrated into Spring Cloud. SCG uses a predicates + filters model: predicates match requests "
            "(Path, Method, Header), filters transform them (RewritePath, AddRequestHeader, RateLimiter). "
            "SCG is the recommended gateway for WebFlux-based microservice architectures and integrates "
            "natively with Spring Cloud LoadBalancer and Eureka."
        ),
    },
    {
        "section": "Spring Boot & Spring Cloud",
        "q": "What is Spring Cloud LoadBalancer and how does it work with Eureka?",
        "a": (
            "Spring Cloud LoadBalancer (SCL) replaced Netflix Ribbon as the default client-side load balancer. "
            "When a WebClient uses a lb:// URI, SCL intercepts the request, queries the Eureka client for "
            "all healthy instances of the service, and picks one using the configured algorithm "
            "(round-robin by default). The Eureka client maintains a local cache of the registry, "
            "refreshed every 30 s, so load balancing decisions are fast (no network call per request). "
            "SCL is integrated automatically when spring-cloud-starter-netflix-eureka-client is on the classpath."
        ),
    },
    {
        "section": "Spring Boot & Spring Cloud",
        "q": "How do Spring profiles work in this project (docker vs default)?",
        "a": (
            "SPRING_PROFILES_ACTIVE=docker activates the docker profile in containers. "
            "When active, Spring merges application-docker.yml (or docker-prefixed properties) with "
            "application.yml -- docker values take precedence. In docker-compose, environment variables "
            "like SPRING_R2DBC_URL override the application.yml defaults. For example, "
            "application.yml has r2dbc:postgresql://localhost:5432 (for local dev) while docker-compose "
            "sets SPRING_R2DBC_URL=r2dbc:postgresql://postgres:5432 (using the Docker service hostname). "
            "This lets the same JAR run locally against localhost and in Docker against container hostnames."
        ),
    },
    {
        "section": "Spring Boot & Spring Cloud",
        "q": "Why does each service have its own ObjectMapper instead of a shared Spring bean?",
        "a": (
            "The project creates ObjectMapper as a field (private final ObjectMapper json = new ObjectMapper()) "
            "because these are simple services that don't need customized serialization config. "
            "In a production codebase you'd inject the Spring-managed ObjectMapper bean -- "
            "Spring Boot autoconfigures it with sensible defaults (JavaTimeModule for dates, "
            "WRITE_DATES_AS_TIMESTAMPS=false, etc.) and any customization (@JsonComponent, "
            "Jackson2ObjectMapperBuilderCustomizer) applies globally. Sharing one instance also avoids "
            "the overhead of ObjectMapper instantiation (it's expensive to construct)."
        ),
    },

    # -- TESTING ---------------------------------------------------------------
    {
        "section": "Testing",
        "q": "How would you test the booking flow end-to-end?",
        "a": (
            "Integration test using @SpringBootTest with Testcontainers: spin up Postgres, Redis, Kafka, "
            "and Cassandra containers. Use WebTestClient to call POST /api/bookings, verify the DB has a "
            "PENDING booking, call the payment endpoint, verify status changes to CONFIRMED, "
            "then consume from the booking-events Kafka topic and verify the notification event was published, "
            "and query Cassandra to verify the history row was written. "
            "Unit tests would use StepVerifier from reactor-test to assert reactive pipelines emit expected "
            "values or errors, with Mockito mocks for repositories."
        ),
    },
    {
        "section": "Testing",
        "q": "How do you test reactive code with Reactor's StepVerifier?",
        "a": (
            "StepVerifier subscribes to a Mono or Flux and asserts the emitted sequence. Example: "
            "StepVerifier.create(service.getHotel(999)).expectError(ApiException.class).verify(). "
            "For a success: StepVerifier.create(service.getHotel(1)).assertNext(h -> assertThat(h.name()) "
            ".isEqualTo('Grand Hotel')).verifyComplete(). "
            "StepVerifier is essential because reactive streams are lazy -- just calling service.getHotel() "
            "returns a Mono without executing anything. StepVerifier forces subscription and verifies "
            "the entire lifecycle: elements emitted, errors thrown, completion signal."
        ),
    },
    {
        "section": "Testing",
        "q": "What is Testcontainers and why is it preferred over H2 for integration tests?",
        "a": (
            "Testcontainers is a Java library that starts real Docker containers for dependencies during tests "
            "and tears them down afterward. It's preferred over H2 (in-memory DB) because: "
            "1) H2 dialect differs from PostgreSQL -- queries that work in Postgres may fail in H2 and vice versa. "
            "2) R2DBC-specific features (e.g., advisory locks, custom types) aren't supported in H2. "
            "3) Cassandra and Kafka have no in-memory equivalents -- Testcontainers gives you the real thing. "
            "Tests run against the same DB engine as production, eliminating a whole class of environment-specific bugs."
        ),
    },
    {
        "section": "Testing",
        "q": "How would you test the Kafka integration in search-service?",
        "a": (
            "Using Testcontainers Kafka module: @Container KafkaContainer kafka = new KafkaContainer(DockerImageName.parse('confluentinc/cp-kafka:7.6.0')). "
            "Start the container, override spring.kafka.bootstrap-servers with kafka.getBootstrapServers(). "
            "Produce a hotel.created message using KafkaTemplate directly in the test. "
            "Wait for the consumer to process it using Awaitility: await().atMost(10, SECONDS).until(() -> "
            "meiliClient.search('Grand Hotel', null).block().hits().size() == 1). "
            "Verify the MeiliSearch index (using a test MeiliSearch container) has the expected document."
        ),
    },

    # -- MONITORING & OBSERVABILITY ---------------------------------------------
    {
        "section": "Monitoring & Observability",
        "q": "How would you add distributed tracing to this system?",
        "a": (
            "Add Micrometer Tracing with Zipkin or Jaeger. Each service gets a trace ID on the first request; "
            "propagated via HTTP headers (traceparent in W3C format or X-B3-TraceId in Zipkin format) to "
            "downstream services. The gateway injects the trace ID if not present. "
            "Kafka message headers carry the trace ID for async propagation. "
            "spring-boot-starter-actuator + micrometer-tracing-bridge-brave + zipkin-reporter-brave "
            "on the classpath auto-instruments WebClient, R2DBC, and Kafka. "
            "Jaeger UI shows the full request tree across all services, making it easy to identify "
            "which service is adding latency."
        ),
    },
    {
        "section": "Monitoring & Observability",
        "q": "What metrics would you expose and monitor for this system?",
        "a": (
            "Spring Boot Actuator with Micrometer auto-exposes: JVM memory/GC, HTTP request rate/latency "
            "(http.server.requests), R2DBC connection pool, Redis connection pool, Kafka consumer lag "
            "(critical -- measures how far behind consumers are), Kafka producer send rate. "
            "Custom business metrics: booking_confirmed_total counter, search_request_latency histogram, "
            "hotel_index_size gauge. Scrape with Prometheus, visualize in Grafana. "
            "Alert on: Kafka consumer lag > 1000 (search-service falling behind), "
            "booking success rate < 99%, R2DBC pool exhausted (p99 latency spike)."
        ),
    },
    {
        "section": "Monitoring & Observability",
        "q": "How would you implement centralized logging?",
        "a": (
            "Configure each service to log in JSON format (Logback with logstash-logback-encoder). "
            "Include trace_id, span_id, service_name, and level in every log line. "
            "Ship logs to an ELK stack (Elasticsearch + Logstash + Kibana) or OpenSearch using "
            "Filebeat as the log shipper from containers. Kibana provides full-text search across "
            "all service logs filtered by trace ID -- you can follow a single request across all 8 services. "
            "Alternatively, use a managed service like Datadog or Splunk. Structured JSON logs are "
            "machine-parseable, enabling alerting on error rate without regex."
        ),
    },

    # -- PERFORMANCE -----------------------------------------------------------
    {
        "section": "Performance",
        "q": "What are the main performance bottlenecks you'd expect and how would you address them?",
        "a": (
            "1) Search -- already mitigated with Redis cache (60s TTL) in front of MeiliSearch. "
            "For very high traffic, add a CDN in front of search results. "
            "2) Room availability query -- 365 rows per room scanned for each availability check. "
            "Mitigate: Redis cache the availability per hotel/date range, invalidated on booking. "
            "3) Booking history in Cassandra -- single-partition reads are fast; "
            "pagination with paging state would be needed for users with thousands of bookings. "
            "4) Kafka consumer lag -- add more partitions and consumer instances. "
            "5) N+1 queries in hotel/room listing -- batch-fetch with IN clauses."
        ),
    },
    {
        "section": "Performance",
        "q": "How would you cache hotel availability to reduce database load?",
        "a": (
            "Cache the availability response in Redis keyed by hotel_id:check_in:check_out with a short TTL "
            "(30 seconds). On booking confirmation, proactively invalidate all cached keys that overlap "
            "the booked date range -- this is complex since the key space is large. "
            "Alternative: use a write-through cache with a 5-second TTL (stale data for 5 s is acceptable "
            "for availability display, but we show real availability at payment time). "
            "Another approach: use a Redis sorted set where scores are dates and values are available counts, "
            "allowing range queries directly in Redis without hitting Postgres."
        ),
    },

    # -- ADVANCED TOPICS -------------------------------------------------------
    {
        "section": "Advanced Topics",
        "q": "What is the outbox pattern and when would you use it here?",
        "a": (
            "The transactional outbox pattern solves the dual-write problem: you can't atomically write to "
            "your DB and publish to Kafka in one transaction (they're different systems). "
            "Solution: add an 'outbox' table to the same DB. In the same transaction that saves the hotel, "
            "also insert a row into the outbox (event type, payload, status=PENDING). "
            "A separate process (Debezium CDC or a scheduler) reads PENDING rows, publishes to Kafka, "
            "marks them SENT. The DB transaction guarantees atomicity -- if the hotel save fails, "
            "the outbox row is also rolled back. Use here for: hotel events (guarantee index stays in sync) "
            "and booking events (guarantee notifications are always sent)."
        ),
    },
    {
        "section": "Advanced Topics",
        "q": "What is the CQRS pattern and does this project use it?",
        "a": (
            "Command Query Responsibility Segregation separates write operations (commands) from read "
            "operations (queries), potentially using different data models optimized for each. "
            "This project partially applies CQRS: hotel-service writes to PostgreSQL (command side), "
            "while search-service reads from MeiliSearch (query side). The two models are kept in sync "
            "via Kafka events. This is the read model projection pattern -- the search index is a "
            "denormalized projection optimized for full-text queries, while Postgres is normalized "
            "for transactional writes. Full CQRS would also separate booking reads from writes."
        ),
    },
    {
        "section": "Advanced Topics",
        "q": "How would you implement a circuit breaker for inter-service calls?",
        "a": (
            "Use Resilience4j's ReactiveCircuitBreaker with Spring WebFlux. Wrap the WebClient call: "
            "circuitBreakerFactory.create('hotel-service').run(webClient.get()...retrieve()..., "
            "throwable -> fallbackMono). States: CLOSED (normal), OPEN (all calls short-circuit to fallback "
            "after failure threshold), HALF_OPEN (probe calls allowed). "
            "Configure: slidingWindowSize=10, failureRateThreshold=50%, waitDurationInOpenState=30s. "
            "This prevents cascading failures -- if hotel-service is slow, other services calling it "
            "degrade gracefully instead of queueing up threads waiting for timeouts."
        ),
    },
    {
        "section": "Advanced Topics",
        "q": "How would you implement an idempotent payment endpoint?",
        "a": (
            "Accept an Idempotency-Key header (a client-generated UUID) on the payment request. "
            "Before processing: check Redis for the key -- if found, return the cached response immediately. "
            "If not found: process the payment, store the response in Redis with key=Idempotency-Key "
            "and TTL=24h, return the response. "
            "This ensures that if the client retries due to a network timeout, the payment is processed "
            "exactly once. The Redis check must happen before any DB write -- use a Lua script for "
            "atomic check-and-set to prevent race conditions between concurrent retries."
        ),
    },
    {
        "section": "Advanced Topics",
        "q": "What is eventual consistency and where does this system accept it?",
        "a": (
            "Eventual consistency means that while data may be temporarily inconsistent across components, "
            "it will converge to a consistent state given enough time. This system accepts it in: "
            "1) Search index -- a newly created hotel may not appear in search results for a few seconds "
            "while the Kafka event is processed; "
            "2) Notifications -- a user may receive their booking confirmation notification seconds after "
            "the booking is confirmed, not atomically; "
            "3) Redis cache -- cached hotel data may be up to 60 s stale after an update. "
            "The booking state itself (PENDING -> CONFIRMED) is strongly consistent via Postgres transactions."
        ),
    },
    {
        "section": "Advanced Topics",
        "q": "How would you handle a complete Kafka cluster failure?",
        "a": (
            "The system is designed to degrade gracefully. kafka.send() failures are caught by onErrorResume "
            "and swallowed -- hotel creation, booking confirmation, and reviews still succeed; only async "
            "side effects (index updates, notifications) are delayed. "
            "When Kafka recovers, no events are replayed automatically (they were lost). "
            "Recovery strategies: 1) Implement the outbox pattern so events are durably stored in DB and "
            "replayed on Kafka recovery; 2) Provide an admin endpoint to re-index all hotels from Postgres "
            "into MeiliSearch; 3) Set acks=all and enable producer retries with idempotent producer for "
            "at-least-once guarantees."
        ),
    },
    {
        "section": "Advanced Topics",
        "q": "What is event sourcing and how does it differ from what you've implemented?",
        "a": (
            "Event sourcing stores the full history of state changes as an immutable sequence of events, "
            "and derives current state by replaying them. The current state is never stored -- it's always computed. "
            "This project uses event-driven architecture (publish events after state changes) but not event sourcing "
            "(we still store current state in Postgres). The booking_history table in Cassandra is append-only "
            "and resembles an event log, but it's a read model, not the source of truth. "
            "True event sourcing would eliminate the Postgres bookings table and derive current booking "
            "state by replaying all booking events from Kafka (with a Kafka topic as the event store)."
        ),
    },
    {
        "section": "Advanced Topics",
        "q": "How would you implement search with availability filtering (available rooms on dates)?",
        "a": (
            "MeiliSearch doesn't natively join with availability data. Two approaches: "
            "1) Denormalized: include min_available and min_price in the MeiliSearch hotel document, "
            "updated via Kafka when availability changes. Filter on min_available > 0. "
            "This is eventual consistency but scales. "
            "2) Two-phase search: query MeiliSearch for hotels matching location/type, then query Postgres "
            "availability for those hotel IDs in parallel (Flux.fromIterable(hotelIds).flatMap()), "
            "filter out unavailable ones, and merge results. This is consistent but adds latency. "
            "The design doc's Phase 2 specifies the first approach."
        ),
    },
    {
        "section": "Advanced Topics",
        "q": "What would you change if traffic scaled 100x?",
        "a": (
            "1) API Gateway: scale horizontally behind ALB, use Redis for rate limiting state. "
            "2) Postgres: read replicas for hotel/review reads, PgBouncer for connection pooling, "
            "partition the bookings table by created_at. "
            "3) Kafka: increase partitions (16-32 per topic), add consumer instances. "
            "4) Search: MeiliSearch single-node may not suffice -- evaluate Elasticsearch with dedicated "
            "master/data nodes or MeiliSearch Cloud. "
            "5) Caching: increase Redis TTLs, add application-level in-process cache (Caffeine) for "
            "hot hotel data. 6) Cassandra: add nodes, use NetworkTopologyStrategy RF=3. "
            "7) Add async booking confirmation instead of synchronous pay-and-confirm."
        ),
    },

    # -- ADDITIONAL PROJECT-SPECIFIC -------------------------------------------
    {
        "section": "Project-Specific Deep Dive",
        "q": "Walk me through what happens when a user searches for hotels in Paris.",
        "a": (
            "1) User calls GET /api/hotels/search?location=Paris with JWT. "
            "2) API Gateway validates JWT, adds X-User-Id, routes to search-service (lb://search-service). "
            "3) SearchService builds cache key 'search:Paris|||||' and checks Redis. "
            "4a) Cache hit: deserialize and return immediately. "
            "4b) Cache miss: call MeiliSearchClient.search('Paris', 'status = verified'). "
            "5) MeiliSearchClient POST /indexes/hotels/search with q=Paris, filter=status='verified'. "
            "6) MeiliSearch returns hits: [{id:1, name:'Grand Paris Hotel', location:'Paris, France', ...}]. "
            "7) Map to SearchResultItem list, wrap in SearchResponse, serialize to Redis (60s TTL), return. "
            "8) Gateway forwards JSON response to client."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "Walk me through what happens when a hotel manager creates a new hotel.",
        "a": (
            "1) Manager calls POST /api/hotels with JWT (role=MANAGER) and hotel details. "
            "2) Gateway validates JWT, routes to hotel-service. "
            "3) HotelService.createHotel() calls HotelVerificationApi.verify(credentials) -- "
            "a mock that returns true for 'valid' credentials, setting status='verified'. "
            "4) Hotel saved to Postgres. toResponse() maps to HotelResponse DTO. "
            "5) HotelEventProducer.sendCreated() publishes hotel.created JSON to hotel-events Kafka topic "
            "with hotelId as key. "
            "6) Response returned to manager immediately. "
            "7) Async: search-service's HotelIndexer consumes hotel.created, calls MeiliSearchClient.upsert(), "
            "hotel appears in search results within seconds."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "Walk me through the complete booking and payment flow.",
        "a": (
            "1) User calls POST /api/hotels/{hotelId}/bookings with room/dates. "
            "2) BookingService creates booking in Postgres with status=PENDING, calls "
            "hotel-service to decrement room availability. "
            "3) User calls POST /api/bookings/{bookingId}/pay. "
            "4) BookingService updates status to CONFIRMED (Postgres transaction). "
            "5) writeHistory() writes a row to Cassandra booking_history (partitioned by userId). "
            "6) BookingEventProducer.sendConfirmed() publishes booking-events message to Kafka. "
            "7) notification-service consumes the event, creates a notification in its Postgres DB. "
            "8) User can fetch notifications via GET /api/users/{userId}/notifications and "
            "history via GET /api/users/{userId}/booking-history."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "How is the HotelIndexer resilient to out-of-order events?",
        "a": (
            "Hotel events are keyed by hotel ID so they always go to the same Kafka partition, "
            "guaranteeing in-order delivery per hotel. However, across hotels there's no ordering guarantee. "
            "The HotelIndexer handles hotel.created and hotel.updated identically (both call upsert) -- "
            "upsert is idempotent, so receiving an 'updated' event before 'created' just overwrites "
            "whatever was there (or creates it if missing). "
            "hotel.deleted calls delete -- if a delete arrives before create (impossible since events "
            "are ordered per hotel), the subsequent upsert would re-add it. "
            "In practice, since ordering is guaranteed per partition, this is not an issue."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "Why does the gateway strip the Authorization header before forwarding?",
        "a": (
            "The gateway is the only trust boundary. Downstream services run in a private network "
            "(Docker bridge network) not reachable from the internet. Stripping the JWT and replacing it "
            "with X-User-Id / X-User-Role headers means: "
            "1) Downstream services don't need the JWT_SECRET -- reducing the blast radius if a service is compromised. "
            "2) Services don't need JWT parsing logic -- simpler code. "
            "3) The authorization model is centralized. "
            "The risk: if a malicious request bypasses the gateway and reaches a service directly, "
            "it could set arbitrary X-User-Id headers. Mitigation: network-level isolation (services "
            "not exposed on the host network) and mutual TLS between services."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "What is the external.mocks.enabled flag in hotel-service and booking-service?",
        "a": (
            "It controls whether external dependency mocks are activated. hotel-service has mocks for "
            "HotelVerificationApi (business credentials verification) and ObjectStorage (S3 presigned URLs). "
            "booking-service has a mock for a payment gateway. When enabled=true (default in dev), "
            "these beans return hardcoded responses: HotelVerificationApi always returns true for 'valid' "
            "credentials, ObjectStorage returns fake URLs, payment gateway approves all payments. "
            "In production (enabled=false), real implementations would call the actual external APIs. "
            "This is the Strangler Fig pattern applied to third-party dependencies."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "What is the purpose of the common module?",
        "a": (
            "The common module is a shared library depended on by all services. It contains: "
            "JwtService -- JWT creation and parsing logic shared between auth-service and api-gateway; "
            "ApiException -- a unified exception class with factory methods (notFound, badRequest, forbidden) "
            "used across all services; GlobalExceptionHandler -- @ControllerAdvice that maps ApiExceptions "
            "to HTTP responses with a consistent JSON body. "
            "Sharing via a common module avoids duplication and ensures consistent error formats "
            "across all APIs. The module is built as part of the parent Maven multi-module project."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "How would you add a new 'payment history' feature to booking-service?",
        "a": (
            "1) Add a payments table to Postgres schema: id, booking_id, amount, status, created_at. "
            "2) Create PaymentEntity, PaymentRepository (ReactiveCrudRepository). "
            "3) In BookingService.pay(), after confirming the booking, also create a Payment row. "
            "4) Add GET /api/bookings/{bookingId}/payments endpoint in BookingController. "
            "5) Add gateway route: Path=/api/bookings/{bookingId}/payments -> lb://booking-service. "
            "6) For user-level payment history, add GET /api/users/{userId}/payments and "
            "a corresponding gateway route. Consider storing payments in Cassandra "
            "partitioned by userId if write volume is very high."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "How does the seed availability logic work and what's its limitation?",
        "a": (
            "When a room is added (addRoom()), seedAvailability() creates 365 RoomAvailability rows -- "
            "one per day for the next year -- each with available_count = room.totalCount. "
            "This is done with availRepo.saveAll(rows) which issues a single batch insert. "
            "Limitation: rooms added today only have availability until next year; on Jan 1, "
            "next year's dates aren't seeded. A cron job would need to extend the window monthly. "
            "Also, 365 inserts per room is expensive for hotels with hundreds of room types. "
            "Production alternative: calculate availability on-the-fly as total_count minus "
            "COUNT(bookings overlapping the range) using a CTE or a summary table."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "How would you implement a hotel deletion feature?",
        "a": (
            "1) Add DELETE /api/hotels/{hotelId} endpoint in HotelController (MANAGER role, owns hotel). "
            "2) In HotelService.deleteHotel(): check the hotel exists and belongs to the manager, "
            "soft-delete by setting status='deleted' (keeps history), invalidate Redis cache for hotel:{id}. "
            "3) Call HotelEventProducer.sendDeleted(hotelId) to publish hotel.deleted to Kafka. "
            "4) HotelIndexer.onHotelEvent() handles 'hotel.deleted' and calls meili.delete(id). "
            "5) Active bookings for this hotel: query and cancel PENDING bookings, notify users. "
            "Soft-delete is preferred over hard-delete to preserve booking history referential integrity."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "What happens if MeiliSearch is down when search-service starts?",
        "a": (
            "MeiliSearchClient.initIndex() runs in @PostConstruct and calls .subscribe() -- it's fire-and-forget. "
            "If MeiliSearch is down at startup, the settings call fails, onErrorResume logs the warning, "
            "and the service starts normally. The index settings (filterable attributes) won't be applied. "
            "Subsequent search calls will also fail gracefully -- onErrorResume in search() returns an "
            "empty MeiliSearchResponse. The service degrades to returning empty results rather than crashing. "
            "When MeiliSearch recovers, new events will index correctly but filterable attributes "
            "would need a manual re-apply or a restart."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "How would you add pagination to the hotel listing endpoint?",
        "a": (
            "Modify GET /api/hotels to accept ?page=0&size=20 parameters. "
            "In HotelController, receive them as @RequestParam. In HotelService.listAll(), "
            "change hotelRepo.findAll() to hotelRepo.findAll(PageRequest.of(page, size)) -- "
            "Spring Data R2DBC's R2dbcRepository supports Pageable. "
            "Return Page<HotelResponse> (or a custom PagedResponse record with items, page, size, total). "
            "For total count, also call hotelRepo.count() and combine with Mono.zip(). "
            "Add a LIMIT/OFFSET query to the custom repository if needed. "
            "For cursor-based pagination (better performance on large datasets), use the last hotel ID "
            "as a cursor: WHERE id > :cursor LIMIT :size."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "Describe a race condition that could occur in the booking flow and how to fix it.",
        "a": (
            "Race condition: two users book the last room simultaneously. Both read available_count=1, "
            "both see it as bookable, both create PENDING bookings. When both call pay(), both decrement "
            "availability -- one succeeds (available_count goes to 0) and the other also 'succeeds' "
            "(available_count goes to -1). Fix: the SQL decrement should be: "
            "UPDATE room_availability SET available_count = available_count - 1 "
            "WHERE room_id = ? AND date = ? AND available_count > 0. "
            "If this updates 0 rows (optimistic locking failure), reject the payment with 409 Conflict. "
            "For the window between booking creation and payment, also check availability in the pay() handler."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "How is the notification-service integrated and what events does it consume?",
        "a": (
            "notification-service consumes two Kafka topics: booking-events (published by booking-service "
            "on booking confirmation) and review-events (published by review-service on review creation). "
            "The NotificationEventConsumer @KafkaListener parses the JSON payload, extracts userId, type, "
            "and message, then calls NotificationService.create() which writes to the notifications Postgres table. "
            "Users can poll GET /api/users/{userId}/notifications to fetch their notifications. "
            "In production you'd add WebSocket or Server-Sent Events push delivery so users don't need to poll."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "What would you add to make this production-ready?",
        "a": (
            "Security: HTTPS/TLS everywhere, network policies (services not directly reachable from internet), "
            "secrets management (Vault or AWS Secrets Manager), input validation with @Valid. "
            "Reliability: circuit breakers (Resilience4j), retries with exponential backoff, "
            "outbox pattern for Kafka publishing, dead-letter topics. "
            "Observability: Micrometer + Prometheus + Grafana, distributed tracing (Zipkin), "
            "structured JSON logging (ELK stack). "
            "Operations: Kubernetes deployment with HPA, Flyway migrations, readiness/liveness probes, "
            "multi-stage Dockerfiles, CI/CD pipeline (GitHub Actions). "
            "Testing: integration tests with Testcontainers, contract tests (Spring Cloud Contract)."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "How would you implement a review rating aggregation for hotels?",
        "a": (
            "Two approaches. 1) Compute on read: SELECT AVG(rating) FROM reviews WHERE hotel_id = ? "
            "on every hotel fetch. Simple but expensive for popular hotels. "
            "2) Materialized aggregate: maintain a hotel_stats table with total_rating, review_count columns. "
            "On each new review, run: UPDATE hotel_stats SET total_rating = total_rating + ?, "
            "review_count = review_count + 1 WHERE hotel_id = ?. Average = total_rating / review_count. "
            "This is an O(1) read. Via Kafka: review-service publishes review.created event, hotel-service "
            "consumes it and updates hotel_stats. Cache the result in Redis. "
            "Include avg_rating in the MeiliSearch hotel document so search can filter/sort by rating."
        ),
    },
    {
        "section": "Project-Specific Deep Dive",
        "q": "How would you implement WebSocket-based real-time notifications?",
        "a": (
            "Replace polling GET /api/users/{userId}/notifications with Server-Sent Events (SSE) or WebSocket. "
            "SSE is simpler: controller returns Flux<Notification> with MediaType.TEXT_EVENT_STREAM_VALUE. "
            "The client opens one long-lived GET connection; the server pushes events as they arrive. "
            "Integration: notification-service's Kafka consumer, instead of only writing to DB, also pushes "
            "to a per-user Sinks.Many<Notification> (a reactive multicast). The SSE endpoint subscribes "
            "to the user's sink. For horizontal scaling, the Sink is per-instance -- use Redis Pub/Sub "
            "to fan-out notifications across all notification-service instances."
        ),
    },
    {
        "section": "Advanced Topics",
        "q": "What is the difference between optimistic and pessimistic locking and which suits booking?",
        "a": (
            "Pessimistic locking: acquire a DB lock (SELECT FOR UPDATE) before reading, hold it until commit. "
            "Prevents concurrent modification but blocks other transactions, reducing throughput. "
            "Optimistic locking: read without locking; at write time, check a version column -- if it changed, "
            "retry. No blocking; works well when conflicts are rare. "
            "For room availability decrement, a conditional UPDATE (WHERE available_count > 0) is essentially "
            "optimistic locking -- it succeeds atomically or fails with 0 rows affected. "
            "This is preferred for booking because: conflicts are rare (most bookings are for different rooms "
            "or dates), and we don't want to hold locks across the check-in/check-out range calculation."
        ),
    },
    {
        "section": "Advanced Topics",
        "q": "How would you implement search autocomplete for hotel names and locations?",
        "a": (
            "MeiliSearch supports this natively. Add a GET /api/hotels/search/autocomplete?q=Par endpoint. "
            "Call POST /indexes/hotels/search with q=Par, attributesToSearchOn=[name, location], limit=5. "
            "MeiliSearch returns the top 5 matching hotels with typo tolerance. "
            "For prefix suggestions, enable prefixSearch: indexingSettings: {prefixSearch: 'indexingTime'}. "
            "Cache autocomplete results in Redis with a very short TTL (5s) since they change often. "
            "Alternatively, use a Redis ZSET (sorted set) with lexicographic scoring for prefix search "
            "on location names -- ZRANGEBYLEX locations '[Par' '[Par\\xff' for instant O(log n) lookups."
        ),
    },
    {
        "section": "Testing",
        "q": "How would you write a contract test between booking-service and hotel-service?",
        "a": (
            "Use Spring Cloud Contract (SCC). hotel-service (the provider) defines contracts in Groovy/YAML "
            "describing the expected request/response: 'given hotel 1 exists, GET /api/hotels/1 returns "
            "{id:1, name:...}'. SCC generates WireMock stubs from these contracts and publishes them. "
            "booking-service (the consumer) uses the stubs in its integration tests to mock hotel-service -- "
            "the test verifies that booking-service calls hotel-service with the correct format. "
            "hotel-service CI also runs the contract as a test against itself. "
            "This ensures both sides of the API contract stay in sync without end-to-end infrastructure."
        ),
    },

    # ----- Liquibase / Database Migrations -----
    {
        "section": "Database Migrations (Liquibase)",
        "q": "Why did you switch from spring.sql.init to Liquibase?",
        "a": (
            "spring.sql.init runs schema.sql every time the app starts. It has no concept of migration history -- "
            "if you change schema.sql, the new statements never run on existing databases (CREATE TABLE IF NOT EXISTS "
            "silently skips them). It's fine for a POC but unsafe in production. Liquibase tracks every applied "
            "changeset in a DATABASECHANGELOG table by (id, author, filename) hash, so each change runs exactly once "
            "per database. To add a column you write V003__add_column.sql; production will pick it up on next deploy "
            "without affecting older environments."
        ),
    },
    {
        "section": "Database Migrations (Liquibase)",
        "q": "Why did you need a custom LiquibaseConfig in Spring Boot 4 with R2DBC?",
        "a": (
            "Liquibase needs a JDBC connection -- it's a synchronous tool that uses java.sql.Connection. "
            "Spring Boot 4's LiquibaseAutoConfiguration only fires when a DataSource bean exists. Pure R2DBC "
            "services have only a ConnectionFactory (reactive), no DataSource, so the autoconfig never triggers "
            "even when spring.liquibase.url is set. Fix: a @Configuration in the common module with "
            "@ConditionalOnClass(SpringLiquibase.class) and @ConditionalOnProperty(prefix = 'spring.liquibase', "
            "name = 'url') that builds a SpringLiquibase bean from a DriverManagerDataSource. Auto-activates for "
            "any service that has liquibase-core on its classpath; silently skipped in gateway/search/eureka."
        ),
    },
    {
        "section": "Database Migrations (Liquibase)",
        "q": "Walk me through your Liquibase changelog structure.",
        "a": (
            "Each service has a master changelog at db/changelog/db.changelog-master.yaml that includes ordered "
            "migration files: V001__initial_schema.sql (DDL) and V002__seed_X.sql (data). "
            "Each migration is a 'formatted SQL' file: the first line is '--liquibase formatted sql', then each "
            "DDL statement is preceded by '--changeset author:id labels:initial comment:Description' and followed "
            "by an optional '--rollback ...' line. Liquibase hashes the changeset by (id, author, file) and refuses "
            "to re-run it. To add a migration: drop V003__... in migrations/, register it in the master, deploy."
        ),
    },
    {
        "section": "Database Migrations (Liquibase)",
        "q": "How would you handle a migration that needs to backfill data for millions of rows?",
        "a": (
            "Don't do it inline in a Liquibase changeset -- it would block startup for minutes and lock tables. "
            "Three patterns: 1) Add the column nullable in V010, deploy. 2) Run an out-of-band batch job (or "
            "a separate migration with runOnChange=true and a Java migration that paginates) to backfill. "
            "3) In V011, add the NOT NULL constraint after backfill is verified. Liquibase supports preconditions "
            "(e.g., <preCondition><sqlCheck>SELECT COUNT(*) FROM x WHERE col IS NULL = 0</sqlCheck>) so V011 fails "
            "fast if backfill isn't complete. Never combine schema change + bulk data update in one transaction."
        ),
    },
    {
        "section": "Database Migrations (Liquibase)",
        "q": "How do you seed data with Liquibase versus seeding Cassandra?",
        "a": (
            "PostgreSQL: a regular Liquibase migration file (V002__seed_X.sql) with INSERT ... ON CONFLICT DO NOTHING "
            "for idempotency. After data inserts I also reset the BIGSERIAL sequence with "
            "setval('table_id_seq', GREATEST(MAX(id), seedMax)) so application-generated IDs don't collide with "
            "seeded IDs. Cassandra has no equivalent migration tool, so we use a CassandraSeedRunner: an "
            "ApplicationRunner bean that checks if seed data already exists (count > 0 for a known partition) and "
            "skips if so, otherwise inserts via the reactive repository. Schema creation happens via Spring Data's "
            "SchemaAction.CREATE_IF_NOT_EXISTS in CassandraConfig."
        ),
    },

    # ----- Jakarta Validation -----
    {
        "section": "Jakarta Validation",
        "q": "How is request validation wired up in this project?",
        "a": (
            "Three pieces: 1) spring-boot-starter-validation on the classpath -- pulls Hibernate Validator, "
            "the Jakarta Validation reference implementation. 2) Constraints on record components: "
            "@NotBlank @Email on email, @Size(min=8, max=72) on password, @Min(1) @Max(5) on rating, etc. "
            "Records work fine with constraints; they apply to the canonical constructor. 3) @Valid on the "
            "@RequestBody parameter in the controller: 'public Mono<X> create(@Valid @RequestBody Y body)'. "
            "Without @Valid the constraints are inert. When validation fails, Spring throws "
            "WebExchangeBindException, caught by GlobalExceptionHandler and converted to a 400 ProblemDetail."
        ),
    },
    {
        "section": "Jakarta Validation",
        "q": "Why is the password capped at 72 characters?",
        "a": (
            "BCrypt has a hard 72-byte input limit -- characters past 72 are silently truncated by the algorithm. "
            "If you accept passwords longer than 72, two different passwords that share the first 72 chars will "
            "hash to the same value. The application currently uses SHA-256 for the POC, which has no such limit, "
            "but I cap at 72 anyway because the design doc calls for BCrypt in production. Keeping the constraint "
            "now means switching the hasher later doesn't introduce a silent security regression."
        ),
    },
    {
        "section": "Jakarta Validation",
        "q": "What's the difference between @NotNull, @NotEmpty, and @NotBlank?",
        "a": (
            "@NotNull: value must not be null; an empty string is fine. "
            "@NotEmpty: value must not be null AND not empty -- works for String, Collection, Map, array. "
            "An all-whitespace string passes. "
            "@NotBlank: String-only; must not be null AND must contain at least one non-whitespace character. "
            "Use @NotBlank for user-facing text fields like name and email (so '   ' is rejected), @NotEmpty for "
            "lists/maps that need at least one entry, and @NotNull for non-string objects you require."
        ),
    },
    {
        "section": "Jakarta Validation",
        "q": "How would you implement a custom validator (e.g., checkOut > checkIn)?",
        "a": (
            "Field-level constraints can't see other fields, so you need a class-level constraint. "
            "1) Create @CheckoutAfterCheckin annotation with @Constraint(validatedBy = CheckoutAfterCheckinValidator.class), "
            "@Target(TYPE), @Retention(RUNTIME). "
            "2) Implement ConstraintValidator<CheckoutAfterCheckin, CreateBookingRequest> with isValid() returning "
            "req.checkOut().isAfter(req.checkIn()). "
            "3) Annotate the record class itself: @CheckoutAfterCheckin public record CreateBookingRequest(...). "
            "Cleaner than throwing ApiException.badRequest in the service because the error surface stays consistent "
            "with all other field validation errors (same VALIDATION_ERROR code in ProblemDetail)."
        ),
    },

    # ----- ProblemDetail (RFC 7807) -----
    {
        "section": "Error Handling (ProblemDetail / RFC 7807)",
        "q": "What is RFC 7807 and why are you using ProblemDetail?",
        "a": (
            "RFC 7807 ('Problem Details for HTTP APIs') is the IETF standard for machine-readable error responses. "
            "It defines a JSON object with type (URI for the problem class), title (human summary), status (HTTP "
            "code), detail (specific message), instance (URI for this occurrence), plus extension members. "
            "Spring 6+ ships ProblemDetail and serializes it as application/problem+json. "
            "Benefits over a custom error DTO: 1) clients (and SDK generators) recognize the shape; "
            "2) the 'type' URI gives you a stable contract per error class -- you can publish docs at that URL; "
            "3) extension fields let you add per-error context (validation field map, retry-after, etc.)."
        ),
    },
    {
        "section": "Error Handling (ProblemDetail / RFC 7807)",
        "q": "Walk me through your GlobalExceptionHandler.",
        "a": (
            "It's a @RestControllerAdvice in the common module, so all services pick it up via classpath scanning. "
            "Four @ExceptionHandler methods: ApiException -> uses the ApiException's status/code/message; "
            "WebExchangeBindException -> 400 with code='VALIDATION_ERROR' and an 'errors' extension member listing "
            "{field, message} per failed constraint; IllegalArgumentException -> 400 with code='BAD_REQUEST'; "
            "Exception (catch-all) -> 500 with code='INTERNAL_ERROR'. "
            "All paths build a ProblemDetail via a shared helper that sets type (URI under "
            "https://api.booking.com/problems/<code>), title, instance (request path), and extension members "
            "code (machine-readable) and timestamp."
        ),
    },
    {
        "section": "Error Handling (ProblemDetail / RFC 7807)",
        "q": "Why do you keep both 'status' and a 'code' extension field?",
        "a": (
            "status is the HTTP status code (400, 404, 500) -- coarse, transport-level. code is a stable, "
            "application-level error identifier (NOT_FOUND, VALIDATION_ERROR, BOOKING_DATES_OVERLAP) that clients "
            "switch on. Status alone isn't enough: 'GET /api/hotels/1 returned 400' tells a client nothing it can "
            "act on -- did the URL parse fail, was the JWT malformed, was a constraint violated? "
            "code lets you change wording in 'detail' without breaking client switch statements. "
            "Mature APIs (Stripe, GitHub) follow this exact pattern."
        ),
    },
    {
        "section": "Error Handling (ProblemDetail / RFC 7807)",
        "q": "How does the ProblemDetail's Content-Type get set to application/problem+json?",
        "a": (
            "Spring's MessageConverter for ProblemDetail (ProblemDetailJacksonMixin + dedicated handler) automatically "
            "negotiates application/problem+json when the controller method returns a ProblemDetail object. "
            "You don't need to manually set Content-Type or wrap the response in ResponseEntity -- though you can "
            "(ResponseEntity<ProblemDetail>) if you also need to add headers like Retry-After. "
            "When the client sends Accept: application/json the converter still uses problem+json because RFC 7807 "
            "explicitly states it's a JSON subtype; well-behaved clients accept it."
        ),
    },

    # ----- Cursor Pagination -----
    {
        "section": "Cursor-Based Pagination",
        "q": "Why cursor pagination instead of offset/page-number pagination?",
        "a": (
            "Offset (LIMIT 20 OFFSET 1000): 1) Performance -- the database must scan and discard 1000 rows to find "
            "row 1001; cost grows linearly with page depth. 2) Stability -- if a row is inserted before the current "
            "page boundary, every subsequent page shifts by one and the user sees an item twice or skips one. "
            "Cursor (WHERE id > 100 LIMIT 20): 1) O(log n) index seek to the cursor regardless of depth; "
            "2) Stable under concurrent inserts because the cursor anchors on a real value, not a position. "
            "Trade-off: no random page jumps ('go to page 50') and no total count without a separate query."
        ),
    },
    {
        "section": "Cursor-Based Pagination",
        "q": "Walk me through your cursor implementation on GET /api/hotels.",
        "a": (
            "Request: GET /api/hotels?cursor=<opaque>&limit=20. The Cursor utility decodes Base64-encoded id "
            "(empty cursor = 0). The repository runs SELECT * FROM hotel WHERE id > :cursor ORDER BY id ASC LIMIT "
            ":limit+1 -- the +1 trick lets us know if more pages exist without a separate COUNT query. "
            "If the result has limit+1 rows, hasMore=true and we return the first 'limit' items. "
            "nextCursor = encoded id of the last returned item. The response is "
            "CursorPageResponse{items, nextCursor, hasMore}. Client treats nextCursor as opaque -- they pass it "
            "back unchanged on the next request."
        ),
    },
    {
        "section": "Cursor-Based Pagination",
        "q": "Why is the cursor Base64-encoded instead of just being the raw ID?",
        "a": (
            "Opacity. If clients see ?cursor=42 they'll start hand-crafting cursors ('?cursor=43 to skip one!'). "
            "When we later change the cursor format -- e.g., to (createdAt, id) tuples for time-ordered listings -- "
            "those clients break. Wrapping in Base64 signals 'this is opaque, don't parse it'. "
            "Same reasoning behind GitHub's REST cursors and Stripe's pagination tokens. "
            "It also lets us swap the encoding (HMAC-signed, encrypted, JSON-encoded multi-field) without changing "
            "the URL parameter shape."
        ),
    },
    {
        "section": "Cursor-Based Pagination",
        "q": "How would you cursor-paginate by something other than id (e.g., review createdAt DESC)?",
        "a": (
            "Use a composite cursor: (createdAt, id) tuple. The query becomes "
            "WHERE (created_at, id) < (:cursorTime, :cursorId) ORDER BY created_at DESC, id DESC LIMIT :n. "
            "id is the tiebreaker for items with identical timestamps -- without it you'd repeat or skip rows. "
            "Encode the tuple as JSON, then Base64: '{\"t\":\"2026-04-01T10:00:00Z\",\"id\":42}' -> opaque token. "
            "Index: CREATE INDEX ON review(created_at DESC, id DESC) so the WHERE clause is an index seek. "
            "Same +1 trick to detect hasMore."
        ),
    },

    # ----- API Gateway / Public Endpoints -----
    {
        "section": "API Gateway / Public Endpoints",
        "q": "Why was the auth filter changed to allow GET /api/hotels without a token?",
        "a": (
            "The frontend needs to display hotels and search results to anonymous visitors -- locking those behind "
            "auth blocks the discovery flow. The filter now allows GET requests under /api/hotels (list, detail, "
            "search, availability, public reviews) without a JWT, but explicitly excludes /api/hotels/{id}/bookings "
            "(that's the manager-only view of a hotel's bookings). "
            "Writes (POST/PUT/DELETE) and any path containing /bookings still require a valid Bearer token. "
            "It's encoded in JwtAuthFilter.isPublic(method, path)."
        ),
    },
    {
        "section": "Database Migrations (Liquibase)",
        "q": "Why use ON CONFLICT DO NOTHING in your seed migrations?",
        "a": (
            "Liquibase already prevents the changeset from running twice (it's tracked in DATABASECHANGELOG by id+author+file hash). "
            "But ON CONFLICT DO NOTHING is defense-in-depth for two real scenarios: 1) Someone runs the seed manually first "
            "(via psql or a previous deploy), then Liquibase tries to apply the same INSERTs -- without ON CONFLICT, the "
            "primary-key violation rolls back the whole changeset and breaks startup. 2) Two replicas race during initial "
            "deploy on a fresh DB -- Liquibase's lock table mostly prevents this, but ON CONFLICT means a race that slips "
            "through doesn't crash either pod. The setval() that follows ensures the auto-increment sequence advances past "
            "the seeded IDs so application-generated rows don't collide."
        ),
    },
    {
        "section": "API Gateway / Public Endpoints",
        "q": "What are the security risks of allowing anonymous reads at the gateway?",
        "a": (
            "1) Scraping -- anyone can pull the full hotel catalog. Mitigations: rate limiting per IP "
            "(Spring Cloud Gateway's RequestRateLimiter), bot-detection headers, robots.txt, CAPTCHA on suspicious "
            "patterns. 2) Cost amplification -- public search hits MeiliSearch and Redis on cache miss; "
            "an attacker could send unique queries to bypass cache. Mitigation: request rate limit, normalize "
            "query params before caching to merge near-duplicate searches, alert on cache hit ratio drop. "
            "3) Information leakage -- GET endpoints must return only public fields. The HotelResponse DTO "
            "is fine, but if we ever add internal fields (e.g., manager email) to the entity, we must not "
            "leak them through the public projection."
        ),
    },

    # ─── Access + Refresh Tokens, Logout via Redis ────────────────────────────
    {
        "section": "Tokens, Logout & Session Lifecycle",
        "q": "Why split into access tokens and refresh tokens instead of one long-lived JWT?",
        "a": (
            "Two competing forces: 1) JWTs are stateless -- the server does no DB lookup per request, but the "
            "downside is they can't be revoked before expiry; 2) you want sessions to last long enough that users "
            "don't constantly log in. A single long-lived JWT (e.g., 7 days) means a stolen token is valid for a "
            "week with no recourse. The split: a short-lived (15 min) access token that's stateless and presented "
            "on every API call, plus a longer-lived (7 day) refresh token that's a high-entropy opaque string "
            "stored in Redis. Compromise of an access token has at most 15 min impact; refresh tokens are "
            "presented far less often and can be revoked instantly because they live in Redis."
        ),
    },
    {
        "section": "Tokens, Logout & Session Lifecycle",
        "q": "Walk me through what happens when a user clicks logout.",
        "a": (
            "Client POSTs /api/auth/logout with the access token in the Authorization header and the refresh token "
            "in the body. auth-service: 1) parses the access token to extract jti and exp; 2) "
            "SET blocklist:{jti} = '1' EX (exp - now) -- the TTL means the entry self-evicts at the moment the "
            "token would have naturally expired anyway, so the blocklist set never grows unbounded; 3) "
            "DEL refresh:{token} to revoke the refresh token. From the next request onward, the gateway's "
            "JwtAuthFilter does EXISTS blocklist:{jti} and rejects with 401 if found. The whole flow is idempotent: "
            "expired access tokens silently no-op, unknown refresh tokens silently no-op."
        ),
    },
    {
        "section": "Tokens, Logout & Session Lifecycle",
        "q": "Why not just store all access tokens in Redis and check them every request?",
        "a": (
            "That gives you an allowlist instead of a blocklist. It would mean a Redis round-trip for every API call "
            "across the entire system -- the allowlist's read load equals total API throughput. Redis becomes a "
            "single point of failure for the entire platform. The blocklist-only approach: in the common case "
            "(user hasn't logged out) Redis returns 'not present' which is a cheap O(1) operation, but the "
            "common-case throughput on Redis is bounded by the rate of *logouts*, not the rate of API calls -- "
            "orders of magnitude smaller. We get the stateless-JWT benefit on the hot path with the revocation "
            "ability of session storage."
        ),
    },
    {
        "section": "Tokens, Logout & Session Lifecycle",
        "q": "Explain refresh-token rotation and why it's important.",
        "a": (
            "Every successful /refresh call deletes the old refresh token from Redis and issues a brand-new one. "
            "Implementation: GETDEL refresh:{token} in a single Redis command (atomic). "
            "Why: it bounds the damage of a stolen refresh token. If an attacker steals a refresh token and uses "
            "it before the legitimate user does, the attacker gets a new pair -- but the original is now invalid, "
            "so the next time the legitimate user tries to refresh they get a 403, which is a clear signal that "
            "their token chain was compromised. They re-login (rotating away from the attacker's session) and "
            "you log/alert on the failed refresh as a security event. Without rotation, a stolen refresh token "
            "is a permanent backdoor for 7 days."
        ),
    },
    {
        "section": "Tokens, Logout & Session Lifecycle",
        "q": "Why is the refresh token an opaque random string and not a JWT?",
        "a": (
            "Three reasons. 1) JWTs encode their claims in the body -- if a refresh token is a JWT and you put "
            "scope/role inside, you can't change those claims without re-issuing the token; whereas an opaque "
            "string just points to a Redis row you can mutate. 2) Refresh tokens already require Redis state to "
            "support revocation/rotation, so the JWT's main selling point (statelessness) is moot -- you'd be "
            "paying the Redis lookup cost AND the JWT verify cost. 3) Smaller payloads on the wire and in "
            "client storage. The auth Bearer token sent on every request stays a JWT (signature verification "
            "is cheaper than a Redis lookup); the refresh token, used rarely, is just an opaque pointer to "
            "Redis-backed session state."
        ),
    },
    {
        "section": "Tokens, Logout & Session Lifecycle",
        "q": "What's the jti claim and why does each token carry one?",
        "a": (
            "jti ('JWT ID', RFC 7519) is a unique identifier for the token -- we use a UUIDv4. It serves two "
            "purposes here: 1) Revocation: the blocklist key is blocklist:{jti}, so revoking one token doesn't "
            "affect other tokens issued for the same user. The user can still have valid sessions on other "
            "devices after logging out on one. 2) Auditability: every authenticated request can be traced to a "
            "specific issued token by jti, which is critical for forensics ('this jti was used at IP X then "
            "again at IP Y -- credential stuffing alert'). Without jti the only token identifier is "
            "(userId, iat) which collides easily."
        ),
    },
    {
        "section": "Tokens, Logout & Session Lifecycle",
        "q": "Why does the blocklist entry have a TTL equal to the token's remaining lifetime?",
        "a": (
            "Because past that moment the access token would have been rejected anyway by signature/expiry "
            "verification, so the blocklist entry no longer needs to exist. This bounds the size of the blocklist: "
            "even with 100% logout rate it can never hold more than (logouts-per-15-minutes) entries at a time. "
            "Without the TTL the blocklist would grow forever and require a separate sweep job -- with the TTL "
            "Redis self-cleans. We compute remaining-ms as (exp - now) when setting the key; if the token is "
            "already expired we skip the SET entirely (nothing to revoke)."
        ),
    },
    {
        "section": "Tokens, Logout & Session Lifecycle",
        "q": "Why does the gateway need access to Redis -- isn't that coupling?",
        "a": (
            "The gateway is the only place we centralize auth, so blocklist enforcement has to happen there. "
            "Pushing the check to each downstream service would mean every team re-implements it and any one "
            "team forgetting it leaves a hole. The coupling cost is small: gateway only does EXISTS, no writes, "
            "no schema. The contract between gateway and auth-service is just the key prefix (TokenKeys.blocklist) "
            "in the common module. If Redis is down, the policy decision is yours: fail-open (let requests "
            "through, good for availability) or fail-closed (reject all auth, good for security). I'd fail-open "
            "with loud alerting -- a Redis outage shouldn't take down the entire API."
        ),
    },
    {
        "section": "Tokens, Logout & Session Lifecycle",
        "q": "How would you implement 'log out from all devices'?",
        "a": (
            "Two approaches. 1) Per-token: track all active jtis for a user under a Redis set "
            "active_tokens:{userId}, blocklist them all on 'logout all'. Linear in number of devices, simple. "
            "2) Generation counter: store a 'min valid iat' per user -- min_iat:{userId} = currentTime. The "
            "gateway checks each token's iat is >= min_iat. Logging out everywhere is a single SET. "
            "Trade-off: approach 2 needs a Redis lookup per request even for users who never logged out anywhere "
            "-- worse hot path. I'd use approach 1 with a Redis set, plus include refresh-token revocation "
            "(SCAN refresh:* and DEL where userId matches -- or maintain a refresh:user:{userId} index for "
            "O(1) lookup)."
        ),
    },
    {
        "section": "Tokens, Logout & Session Lifecycle",
        "q": "How do tokens get to the client, and where should the client store them?",
        "a": (
            "Tokens come back in the JSON response body of /login, /register, and /refresh: "
            "{tokenType, accessToken, accessExpiresIn, refreshToken, refreshExpiresIn, userId, role}. "
            "Client storage choice depends on threat model. For a web app: access token in memory only "
            "(not localStorage -- XSS would steal it), refresh token in an HttpOnly + Secure + SameSite=Strict "
            "cookie (XSS can't read it, CSRF prevented by SameSite). For a mobile app: both in the secure "
            "OS keystore (Keychain on iOS, Keystore on Android). Never log tokens. On 401, the client "
            "transparently calls /refresh and retries the original request once."
        ),
    },
]


L_MARGIN = 20
R_MARGIN = 20
T_MARGIN = 20

class PDF(FPDF):
    def __init__(self):
        super().__init__()
        self.set_auto_page_break(auto=True, margin=15)

    def _eff_w(self):
        return self.w - L_MARGIN - R_MARGIN

    def header(self):
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(100, 100, 100)
        self.set_xy(L_MARGIN, 10)
        self.cell(self._eff_w(), 8, "Hotel Booking System -- Backend Interview Q&A", align="C", new_x="LMARGIN", new_y="NEXT")
        self.ln(2)

    def footer(self):
        self.set_y(-12)
        self.set_font("Helvetica", "", 8)
        self.set_text_color(150, 150, 150)
        self.set_x(L_MARGIN)
        self.cell(self._eff_w(), 8, f"Page {self.page_no()}", align="C")

    def chapter_title(self, text):
        self.set_x(L_MARGIN)
        self.set_font("Helvetica", "B", 13)
        self.set_fill_color(30, 80, 160)
        self.set_text_color(255, 255, 255)
        self.cell(self._eff_w(), 9, f"  {text}", fill=True, new_x="LMARGIN", new_y="NEXT")
        self.ln(3)
        self.set_text_color(0, 0, 0)

    def question_block(self, number, question, answer):
        self.set_x(L_MARGIN)
        self.set_font("Helvetica", "B", 10.5)
        self.set_fill_color(240, 245, 255)
        self.set_text_color(20, 60, 140)
        q_text = f"Q{number}. {question}"
        self.multi_cell(self._eff_w(), 7, q_text, fill=True, new_x="LMARGIN", new_y="NEXT")
        self.ln(1)
        self.set_x(L_MARGIN)
        self.set_font("Helvetica", "", 9.5)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self._eff_w(), 6, answer, new_x="LMARGIN", new_y="NEXT")
        self.ln(4)


def centered_text(pdf, w, h, text, new_line=True):
    pdf.set_x(L_MARGIN)
    if new_line:
        pdf.multi_cell(w, h, text, align="C", new_x="LMARGIN", new_y="NEXT")
    else:
        pdf.cell(w, h, text, align="C", new_x="LMARGIN", new_y="NEXT")


def build_pdf(output_path: str):
    pdf = PDF()
    pdf.set_title("Hotel Booking System -- 100 Backend Interview Questions")
    pdf.set_author("Interview Preparation Guide")
    pdf.set_margins(L_MARGIN, T_MARGIN, R_MARGIN)

    W = 210 - L_MARGIN - R_MARGIN  # effective width mm

    # Cover page
    pdf.add_page()
    pdf.set_font("Helvetica", "B", 22)
    pdf.set_text_color(20, 60, 140)
    pdf.ln(25)
    centered_text(pdf, W, 14, "Hotel Booking System")
    pdf.set_font("Helvetica", "B", 16)
    pdf.set_text_color(60, 60, 60)
    centered_text(pdf, W, 10, "100 Backend Interview Questions & Answers")
    pdf.ln(8)
    pdf.set_font("Helvetica", "", 11)
    pdf.set_text_color(100, 100, 100)
    centered_text(pdf, W, 7,
        "Covers: System Design, Reactive Programming, Kafka, Cassandra,\n"
        "MeiliSearch, Redis, PostgreSQL, Spring Boot 4, Docker, Security")
    pdf.ln(12)
    pdf.set_draw_color(30, 80, 160)
    pdf.set_line_width(0.8)
    y = pdf.get_y()
    pdf.line(L_MARGIN, y, 210 - R_MARGIN, y)
    pdf.ln(10)
    pdf.set_font("Helvetica", "I", 10)
    pdf.set_text_color(120, 120, 120)
    centered_text(pdf, W, 6,
        "Tech stack: Java 21, Spring Boot 4, Spring WebFlux, Spring Cloud Gateway,\n"
        "PostgreSQL, R2DBC, Redis, Apache Kafka (KRaft), Apache Cassandra, MeiliSearch, Docker Compose")

    # Table of contents
    pdf.ln(14)
    sections_seen = []
    for qa in QA:
        if qa["section"] not in sections_seen:
            sections_seen.append(qa["section"])

    pdf.set_x(L_MARGIN)
    pdf.set_font("Helvetica", "B", 12)
    pdf.set_text_color(30, 30, 30)
    pdf.cell(W, 8, "Sections", new_x="LMARGIN", new_y="NEXT")
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(60, 60, 60)
    for i, sec in enumerate(sections_seen, 1):
        count = sum(1 for q in QA if q["section"] == sec)
        pdf.set_x(L_MARGIN)
        pdf.cell(W, 6, f"  {i}. {sec}  ({count} questions)", new_x="LMARGIN", new_y="NEXT")

    # Questions pages
    pdf.add_page()
    current_section = None
    q_num = 0
    for qa in QA:
        if qa["section"] != current_section:
            current_section = qa["section"]
            pdf.chapter_title(current_section)
        q_num += 1
        pdf.question_block(q_num, qa["q"], qa["a"])

    pdf.output(output_path)
    print(f"PDF saved: {output_path}  ({q_num} questions, {pdf.page_no()} pages)")


if __name__ == "__main__":
    build_pdf("/Users/tasnim/Dev/system-design-with-code/booking/interview-questions.pdf")
