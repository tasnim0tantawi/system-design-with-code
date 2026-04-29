package com.booking.review.service;

import com.booking.common.exception.ApiException;
import com.booking.review.dto.ReviewDtos.*;
import com.booking.review.entity.Review;
import com.booking.review.entity.ReviewAggregate;
import com.booking.review.external.ObjectStorage;
import com.booking.review.repository.ReviewAggregateRepository;
import com.booking.review.repository.ReviewRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReviewService {

    private final ReviewRepository repo;
    private final ReviewAggregateRepository aggRepo;
    private final BookingClient bookings;
    private final ReviewEventProducer events;
    private final ObjectStorage storage;
    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper json = new ObjectMapper();

    public ReviewService(ReviewRepository repo, ReviewAggregateRepository aggRepo,
                          BookingClient bookings, ReviewEventProducer events,
                          ObjectStorage storage, ReactiveStringRedisTemplate redis) {
        this.repo = repo;
        this.aggRepo = aggRepo;
        this.bookings = bookings;
        this.events = events;
        this.storage = storage;
        this.redis = redis;
    }

    public Mono<ReviewResponse> create(int userId, int hotelId, CreateReviewRequest req) {
        if (req.rating() == null || req.rating() < 1 || req.rating() > 5) {
            return Mono.error(ApiException.badRequest("rating must be 1..5"));
        }
        return bookings.getBooking(req.bookingId())
                .switchIfEmpty(Mono.error(ApiException.notFound("booking " + req.bookingId())))
                .flatMap(b -> {
                    if (!"completed".equalsIgnoreCase(b.status()) && !"confirmed".equalsIgnoreCase(b.status())) {
                        // POC: accept confirmed too so happy-path curl flow works without
                        // a check-in/check-out simulation step. Documented in README.
                        return Mono.error(ApiException.forbidden("booking not eligible (status=" + b.status() + ")"));
                    }
                    if (b.userId() != userId) {
                        return Mono.error(ApiException.forbidden("not your booking"));
                    }
                    if (b.hotelId() != hotelId) {
                        return Mono.error(ApiException.badRequest("hotel mismatch"));
                    }
                    return repo.existsByUserIdAndBookingId(userId, req.bookingId())
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(ApiException.conflict("review already exists for this booking"));
                                }
                                Review r = new Review();
                                r.setUserId(userId);
                                r.setHotelId(hotelId);
                                r.setBookingId(req.bookingId());
                                r.setRating(req.rating());
                                r.setComment(req.comment());
                                r.setCreatedAt(Instant.now());
                                return repo.save(r);
                            });
                })
                // POC deviation: aggregates are updated synchronously (design says async via Kafka).
                .flatMap(saved -> aggRepo.upsertOnReviewCreated(hotelId, saved.getRating())
                        .then(redis.delete("review-summary:" + hotelId))
                        .then(events.sendReviewCreated(saved.getUserId(), saved.getId()))
                        .thenReturn(saved))
                .map(this::toResponse);
    }

    public Mono<ReviewListResponse> listForHotel(int hotelId) {
        return repo.findByHotelIdOrderByCreatedAtDesc(hotelId)
                .map(this::toResponse)
                .collectList()
                .map(items -> new ReviewListResponse(items, items.size()));
    }

    public Mono<SummaryResponse> summary(int hotelId) {
        String key = "review-summary:" + hotelId;
        return redis.opsForValue().get(key)
                .flatMap(this::deserialize)
                .switchIfEmpty(
                        aggRepo.findById(hotelId)
                                .map(this::toSummary)
                                .defaultIfEmpty(new SummaryResponse(hotelId, 0, 0.0, emptyHistogram()))
                                .flatMap(resp -> serialize(resp)
                                        .flatMap(s -> redis.opsForValue().set(key, s, Duration.ofSeconds(60)))
                                        .thenReturn(resp))
                );
    }

    public Mono<Void> delete(int reviewId, int actingUserId) {
        return repo.findById(reviewId)
                .switchIfEmpty(Mono.error(ApiException.notFound("review " + reviewId)))
                .flatMap(r -> {
                    if (r.getUserId() != actingUserId) {
                        return Mono.error(ApiException.forbidden("not your review"));
                    }
                    return repo.deleteById(reviewId);
                });
    }

    public Mono<PresignResponse> presign(String contentType) {
        return storage.presign(contentType)
                .map(p -> new PresignResponse(p.key(), p.publicUrl(), p.uploadUrl()));
    }

    private SummaryResponse toSummary(ReviewAggregate a) {
        Map<Integer, Integer> hist = new LinkedHashMap<>();
        hist.put(1, nz(a.getRating1())); hist.put(2, nz(a.getRating2()));
        hist.put(3, nz(a.getRating3())); hist.put(4, nz(a.getRating4()));
        hist.put(5, nz(a.getRating5()));
        int total = nz(a.getTotalReviews());
        long sum = a.getSumRatings() == null ? 0 : a.getSumRatings();
        double avg = total == 0 ? 0.0 : ((double) sum) / total;
        return new SummaryResponse(a.getHotelId(), total, avg, hist);
    }

    private Map<Integer, Integer> emptyHistogram() {
        Map<Integer, Integer> m = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) m.put(i, 0);
        return m;
    }

    private int nz(Integer v) { return v == null ? 0 : v; }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getUserId(), r.getHotelId(), r.getBookingId(),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }

    private Mono<String> serialize(SummaryResponse r) {
        try { return Mono.just(json.writeValueAsString(r)); }
        catch (Exception e) { return Mono.error(e); }
    }

    private Mono<SummaryResponse> deserialize(String s) {
        try { return Mono.just(json.readValue(s, new TypeReference<SummaryResponse>() {})); }
        catch (Exception e) { return Mono.empty(); }
    }
}
