package com.booking.review.controller;

import com.booking.common.security.AuthHeaders;
import com.booking.review.dto.ReviewDtos.*;
import com.booking.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Tag(name = "Reviews", description = "Hotel reviews and rating aggregates")
@RestController
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @Operation(summary = "Submit a review for a hotel")
    @PostMapping("/api/hotels/{hotelId}/reviews")
    public Mono<ReviewResponse> create(ServerHttpRequest req, @PathVariable int hotelId,
                                        @Valid @RequestBody CreateReviewRequest body) {
        long uid = AuthHeaders.requireUserId(req);
        return service.create((int) uid, hotelId, body);
    }

    @Operation(summary = "List all reviews for a hotel")
    @GetMapping("/api/hotels/{hotelId}/reviews")
    public Mono<ReviewListResponse> list(@PathVariable int hotelId) {
        return service.listForHotel(hotelId);
    }

    @Operation(summary = "Get rating summary and distribution for a hotel")
    @GetMapping("/api/hotels/{hotelId}/reviews/summary")
    public Mono<SummaryResponse> summary(@PathVariable int hotelId) {
        return service.summary(hotelId);
    }

    @Operation(summary = "Delete own review")
    @DeleteMapping("/api/reviews/{reviewId}")
    public Mono<Void> delete(ServerHttpRequest req, @PathVariable int reviewId) {
        long uid = AuthHeaders.requireUserId(req);
        return service.delete(reviewId, (int) uid);
    }

    @Operation(summary = "Get a pre-signed URL to upload a review image")
    @PostMapping("/api/reviews/images/presign")
    public Mono<PresignResponse> presign(@RequestBody(required = false) Map<String, String> body) {
        String ct = body == null ? "image/jpeg" : body.getOrDefault("contentType", "image/jpeg");
        return service.presign(ct);
    }
}
