package com.booking.review.repository;

import com.booking.review.entity.Review;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReviewRepository extends ReactiveCrudRepository<Review, Integer> {
    Flux<Review> findByHotelIdOrderByCreatedAtDesc(Integer hotelId);
    Mono<Boolean> existsByUserIdAndBookingId(Integer userId, Integer bookingId);
}
