package com.booking.urlshortener.cassandra;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import reactor.core.publisher.Flux;

public interface ClickEventRepository extends ReactiveCassandraRepository<ClickEvent, ClickEventKey> {
    Flux<ClickEvent> findByKeyShortCode(String shortCode);
}
