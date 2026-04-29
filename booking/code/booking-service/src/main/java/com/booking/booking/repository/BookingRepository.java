package com.booking.booking.repository;

import com.booking.booking.entity.Booking;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface BookingRepository extends ReactiveCrudRepository<Booking, Integer> {
    Flux<Booking> findByUserId(Integer userId);
    Flux<Booking> findByHotelId(Integer hotelId);
}
