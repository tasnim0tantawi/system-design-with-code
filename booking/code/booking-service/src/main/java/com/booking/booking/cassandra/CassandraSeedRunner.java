package com.booking.booking.cassandra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Seeds Cassandra booking_history with sample rows on startup.
 *
 * Idempotent: checks if the table already has any rows for our seed user IDs
 * and skips if so. Cassandra has no native migration tool like Liquibase, so a
 * runner is the standard pattern here. Schema creation is handled separately
 * by Spring Data's SchemaAction.CREATE_IF_NOT_EXISTS in CassandraConfig.
 */
@Component
public class CassandraSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CassandraSeedRunner.class);

    private final BookingHistoryRepository repo;

    public CassandraSeedRunner(BookingHistoryRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Idempotency check: if user 4 already has history rows, assume seeded
        Long existing = repo.findByKeyUserId(4).count().block();
        if (existing != null && existing > 0) {
            log.info("Cassandra booking_history already seeded ({} rows for user 4), skipping", existing);
            return;
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.now();

        // Only terminal states (COMPLETED / CANCELED) belong in booking_history --
        // active bookings (PENDING / CONFIRMED) live only in PostgreSQL until they
        // reach a final state.
        List<BookingHistory> seed = List.of(
            // Tasnim (userId=4) -- 2 completed stays
            row(4, 1, 1, 1,  today.plusDays(7),  today.plusDays(10), BookingHistoryStatus.COMPLETED, "660.00",  now.minus(5, ChronoUnit.DAYS)),
            row(4, 2, 5, 10, today.plusDays(30), today.plusDays(33), BookingHistoryStatus.COMPLETED, "870.00",  now.minus(3, ChronoUnit.DAYS)),

            // Amira (userId=5) -- 1 completed
            row(5, 3, 3, 7,  today.plusDays(14), today.plusDays(18), BookingHistoryStatus.COMPLETED, "1040.00", now.minus(2, ChronoUnit.DAYS)),

            // Mariam (userId=6) -- 1 cancelled, 1 completed
            row(6, 5, 2, 4,  today.plusDays(21), today.plusDays(24), BookingHistoryStatus.CANCELED,  "420.00",  now.minus(7, ChronoUnit.DAYS)),
            row(6, 6, 4, 9,  today.plusDays(5),  today.plusDays(8),  BookingHistoryStatus.COMPLETED, "330.00",  now.minus(6, ChronoUnit.HOURS))
        );

        Long saved = repo.saveAll(Flux.fromIterable(seed)).count().block();
        log.info("Cassandra booking_history seeded with {} rows", saved);
    }

    private BookingHistory row(int userId, int bookingId, int hotelId, int roomId,
                                LocalDate checkIn, LocalDate checkOut, BookingHistoryStatus status,
                                String totalPrice, Instant createdAt) {
        BookingHistory bh = new BookingHistory();
        bh.setKey(new BookingHistoryKey(userId, createdAt, bookingId));
        bh.setHotelId(hotelId);
        bh.setRoomId(roomId);
        bh.setCheckIn(checkIn);
        bh.setCheckOut(checkOut);
        bh.setStatus(status);
        bh.setTotalPrice(new BigDecimal(totalPrice));
        return bh;
    }
}
