package com.booking.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ReviewDtos {

    public record CreateReviewRequest(
            @NotNull @Positive Integer bookingId,
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 2000) String comment
    ) {}

    public record ReviewResponse(Integer id, Integer userId, Integer hotelId, Integer bookingId,
                                 Integer rating, String comment, Instant createdAt) {}

    public record SummaryResponse(Integer hotelId, int totalReviews, double avgRating,
                                  Map<Integer, Integer> histogram) {}

    public record ReviewListResponse(List<ReviewResponse> items, int total) {}

    public record PresignResponse(String key, String publicUrl, String uploadUrl) {}

    /** Subset of booking-service GET /api/bookings/{id} response. */
    public record BookingView(@JsonProperty("id") Integer id,
                               @JsonProperty("userId") Integer userId,
                               @JsonProperty("hotelId") Integer hotelId,
                               @JsonProperty("status") String status) {}

    private ReviewDtos() {}
}
