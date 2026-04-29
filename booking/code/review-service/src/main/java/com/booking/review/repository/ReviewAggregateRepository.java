package com.booking.review.repository;

import com.booking.review.entity.ReviewAggregate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ReviewAggregateRepository extends ReactiveCrudRepository<ReviewAggregate, Integer> {

    @Query("INSERT INTO review_aggregate (hotel_id, total_reviews, sum_ratings, rating_1, rating_2, rating_3, rating_4, rating_5) " +
            "VALUES (:hotelId, 1, :rating, " +
            "  CASE WHEN :rating = 1 THEN 1 ELSE 0 END, " +
            "  CASE WHEN :rating = 2 THEN 1 ELSE 0 END, " +
            "  CASE WHEN :rating = 3 THEN 1 ELSE 0 END, " +
            "  CASE WHEN :rating = 4 THEN 1 ELSE 0 END, " +
            "  CASE WHEN :rating = 5 THEN 1 ELSE 0 END) " +
            "ON CONFLICT (hotel_id) DO UPDATE SET " +
            "  total_reviews = review_aggregate.total_reviews + 1, " +
            "  sum_ratings   = review_aggregate.sum_ratings + EXCLUDED.sum_ratings, " +
            "  rating_1      = review_aggregate.rating_1 + EXCLUDED.rating_1, " +
            "  rating_2      = review_aggregate.rating_2 + EXCLUDED.rating_2, " +
            "  rating_3      = review_aggregate.rating_3 + EXCLUDED.rating_3, " +
            "  rating_4      = review_aggregate.rating_4 + EXCLUDED.rating_4, " +
            "  rating_5      = review_aggregate.rating_5 + EXCLUDED.rating_5")
    Mono<Integer> upsertOnReviewCreated(Integer hotelId, Integer rating);
}
