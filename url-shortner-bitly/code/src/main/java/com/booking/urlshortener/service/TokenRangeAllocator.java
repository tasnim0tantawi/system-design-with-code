package com.booking.urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Doles out monotonically increasing IDs without a DB hit per call. On startup
 * (and whenever the in-memory range is exhausted) it does a single atomic
 * UPDATE...RETURNING on the {@code token_range} singleton row to reserve the
 * next batch for this pod.
 *
 * <p>Multiple replicas can run concurrently; the atomic UPDATE guarantees each
 * pod gets a non-overlapping range. A pod crash loses up to {@code batchSize - 1}
 * IDs -- intentional; the design accepts the gap to avoid the
 * complexity of tracking it.</p>
 *
 * <p>The "many concurrent next() callers all see exhausted range" case is handled
 * by a shared in-flight {@link Mono} -- only one DB round-trip happens, all
 * concurrent callers attach to the same publisher.</p>
 */
@Component
public class TokenRangeAllocator {

    private static final Logger log = LoggerFactory.getLogger(TokenRangeAllocator.class);

    private final DatabaseClient db;
    private final long batchSize;

    private final AtomicLong next = new AtomicLong(Long.MAX_VALUE);  // start exhausted -> first call refreshes
    private volatile long endExclusive = 0L;
    private final AtomicReference<Mono<Void>> inFlight = new AtomicReference<>();

    public TokenRangeAllocator(DatabaseClient db, @Value("${tokens.batch-size:1000}") long batchSize) {
        this.db = db;
        this.batchSize = batchSize;
    }

    public Mono<Long> next() {
        long candidate = next.getAndIncrement();
        if (candidate < endExclusive) {
            return Mono.just(candidate);
        }
        // Exhausted -- refresh, then retry
        return refresh().then(Mono.defer(this::next));
    }

    /**
     * Coalesces concurrent refresh attempts into a single DB round-trip.
     */
    private Mono<Void> refresh() {
        Mono<Void> shared = inFlight.get();
        if (shared != null) return shared;

        Mono<Void> created = doRefresh()
                .doFinally(s -> inFlight.set(null))
                .cache();

        if (inFlight.compareAndSet(null, created)) {
            return created;
        }
        return inFlight.get();
    }

    private Mono<Void> doRefresh() {
        return db.sql("UPDATE token_range SET next_value = next_value + :b WHERE id = 1 RETURNING next_value")
                .bind("b", batchSize)
                .map((row, meta) -> row.get(0, Long.class))
                .one()
                .doOnNext(newEnd -> {
                    long start = newEnd - batchSize;
                    next.set(start);
                    endExclusive = newEnd;
                    log.info("token range refreshed: [{}, {})", start, newEnd);
                })
                .then();
    }
}
