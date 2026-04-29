package com.booking.hotel.repository;

import com.booking.hotel.entity.Hotel;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface HotelRepository extends ReactiveCrudRepository<Hotel, Integer> {
    Flux<Hotel> findAllByLocationContainingIgnoreCase(String location);

    @Query("SELECT * FROM hotel WHERE id > :cursor ORDER BY id ASC LIMIT :limit")
    Flux<Hotel> findPage(long cursor, int limit);
}
