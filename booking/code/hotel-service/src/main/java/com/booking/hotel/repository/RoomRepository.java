package com.booking.hotel.repository;

import com.booking.hotel.entity.Room;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface RoomRepository extends ReactiveCrudRepository<Room, Integer> {
    Flux<Room> findByHotelId(Integer hotelId);
}
