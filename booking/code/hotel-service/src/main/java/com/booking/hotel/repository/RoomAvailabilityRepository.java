package com.booking.hotel.repository;

import com.booking.hotel.entity.RoomAvailability;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface RoomAvailabilityRepository extends ReactiveCrudRepository<RoomAvailability, Integer> {

    Flux<RoomAvailability> findByRoomIdAndDateBetween(Integer roomId, LocalDate from, LocalDate to);

    @Query("UPDATE room_availability SET available_count = available_count - 1 " +
            "WHERE room_id = :roomId AND date >= :from AND date < :to AND available_count > 0")
    Mono<Integer> decrement(Integer roomId, LocalDate from, LocalDate to);
}
