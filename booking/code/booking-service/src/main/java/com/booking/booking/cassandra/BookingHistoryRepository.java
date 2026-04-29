package com.booking.booking.cassandra;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import reactor.core.publisher.Flux;

public interface BookingHistoryRepository extends ReactiveCassandraRepository<BookingHistory, BookingHistoryKey> {
    Flux<BookingHistory> findByKeyUserId(int userId);
}
