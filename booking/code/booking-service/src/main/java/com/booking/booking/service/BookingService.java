package com.booking.booking.service;

import com.booking.booking.cassandra.BookingHistory;
import com.booking.booking.cassandra.BookingHistoryKey;
import com.booking.booking.cassandra.BookingHistoryRepository;
import com.booking.booking.cassandra.BookingHistoryStatus;
import com.booking.booking.dto.BookingDtos.BookingResponse;
import com.booking.booking.dto.BookingDtos.CreateBookingRequest;
import com.booking.booking.dto.BookingDtos.PaymentResponse;
import com.booking.booking.entity.Booking;
import com.booking.booking.entity.BookingStatus;
import com.booking.booking.external.PaymentGateway;
import com.booking.booking.repository.BookingRepository;
import com.booking.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    /** POC: 60s instead of 5min for convenience. */
    private static final Duration PENDING_TTL = Duration.ofSeconds(60);

    private final BookingRepository repo;
    private final HotelClient hotelClient;
    private final PaymentGateway payments;
    private final BookingEventProducer events;
    private final BookingHistoryRepository historyRepo;
    private final ReactiveStringRedisTemplate redis;

    public BookingService(BookingRepository repo, HotelClient hotelClient,
                          PaymentGateway payments, BookingEventProducer events,
                          BookingHistoryRepository historyRepo,
                          ReactiveStringRedisTemplate redis) {
        this.repo = repo;
        this.hotelClient = hotelClient;
        this.payments = payments;
        this.events = events;
        this.historyRepo = historyRepo;
        this.redis = redis;
    }

    public Mono<BookingResponse> create(int userId, int hotelId, CreateBookingRequest req) {
        if (!req.checkOut().isAfter(req.checkIn())) {
            return Mono.error(ApiException.badRequest("check_out must be after check_in"));
        }
        return hotelClient.getAvailability(hotelId, req.checkIn(), req.checkOut())
                .flatMap(av -> av.rooms().stream()
                        .filter(r -> r.roomId().equals(req.roomId()))
                        .findFirst()
                        .map(r -> {
                            if (r.minAvailable() <= 0) {
                                return Mono.<BookingResponse>error(ApiException.conflict("room not available"));
                            }
                            Booking b = new Booking();
                            b.setUserId(userId);
                            b.setHotelId(hotelId);
                            b.setRoomId(req.roomId());
                            b.setCheckIn(req.checkIn());
                            b.setCheckOut(req.checkOut());
                            b.setStatus(BookingStatus.PENDING);
                            b.setTotalPrice(r.totalPrice());
                            b.setCreatedAt(Instant.now());
                            b.setUpdatedAt(Instant.now());
                            return repo.save(b)
                                    .flatMap(saved -> redis.opsForValue()
                                            .set("pending-booking:" + saved.getId(),
                                                    String.valueOf(saved.getId()), PENDING_TTL)
                                            .thenReturn(saved))
                                    .map(this::toResponse);
                        })
                        .orElse(Mono.error(ApiException.notFound("room " + req.roomId()))));
    }

    public Mono<PaymentResponse> pay(int bookingId) {
        return repo.findById(bookingId)
                .switchIfEmpty(Mono.error(ApiException.notFound("booking " + bookingId)))
                .flatMap(b -> {
                    if (b.getStatus() != BookingStatus.PENDING) {
                        return Mono.error(ApiException.conflict("booking not in pending state"));
                    }
                    return payments.charge(bookingId, b.getTotalPrice())
                            .flatMap(res -> {
                                if (!res.success()) {
                                    b.setStatus(BookingStatus.CANCELLED);
                                    b.setUpdatedAt(Instant.now());
                                    return repo.save(b)
                                            .then(redis.delete("pending-booking:" + bookingId))
                                            .then(writeHistory(b, BookingHistoryStatus.CANCELED))
                                            .thenReturn(new PaymentResponse(bookingId, false,
                                                    BookingStatus.CANCELLED, null, res.failureReason()));
                                }
                                b.setStatus(BookingStatus.CONFIRMED);
                                b.setUpdatedAt(Instant.now());
                                return repo.save(b)
                                        .then(redis.delete("pending-booking:" + bookingId))
                                        .then(hotelClient.decrement(b.getRoomId(), b.getCheckIn(), b.getCheckOut()))
                                        .then(writeHistory(b, BookingHistoryStatus.COMPLETED))
                                        .then(events.sendConfirmed(b.getUserId(), bookingId))
                                        .thenReturn(new PaymentResponse(bookingId, true,
                                                BookingStatus.CONFIRMED, res.chargeId(), null));
                            });
                });
    }

    public Mono<BookingResponse> get(int id) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(ApiException.notFound("booking " + id)))
                .map(this::toResponse);
    }

    public Flux<BookingResponse> listByUser(int userId) {
        return repo.findByUserId(userId).map(this::toResponse);
    }

    public Flux<BookingHistory> historyByUser(int userId) {
        return historyRepo.findByKeyUserId(userId);
    }

    public Mono<BookingResponse> cancel(int id, int actingUserId) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(ApiException.notFound("booking " + id)))
                .flatMap(b -> {
                    if (b.getUserId() != actingUserId) {
                        return Mono.error(ApiException.forbidden("not your booking"));
                    }
                    BookingStatus prev = b.getStatus();
                    b.setStatus(BookingStatus.CANCELLED);
                    b.setUpdatedAt(Instant.now());
                    return repo.save(b)
                            .flatMap(saved -> redis.delete("pending-booking:" + id).thenReturn(saved))
                            .flatMap(saved -> {
                                if (prev == BookingStatus.CONFIRMED) {
                                    return payments.refund(id, saved.getTotalPrice()).thenReturn(saved);
                                }
                                return Mono.just(saved);
                            })
                            .flatMap(saved -> writeHistory(saved, BookingHistoryStatus.CANCELED).thenReturn(saved))
                            .map(this::toResponse);
                });
    }

    /**
     * Append-only write to the Cassandra {@code booking_history} table.
     * Only ever called with a terminal {@link BookingHistoryStatus} -- the
     * history table doesn't carry intermediate states like PENDING / CONFIRMED.
     */
    private Mono<Void> writeHistory(Booking b, BookingHistoryStatus status) {
        BookingHistory h = new BookingHistory();
        h.setKey(new BookingHistoryKey(b.getUserId(), b.getCreatedAt(), b.getId()));
        h.setHotelId(b.getHotelId());
        h.setRoomId(b.getRoomId());
        h.setCheckIn(b.getCheckIn());
        h.setCheckOut(b.getCheckOut());
        h.setStatus(status);
        h.setTotalPrice(b.getTotalPrice() == null ? BigDecimal.ZERO : b.getTotalPrice());
        return historyRepo.save(h)
                .doOnError(e -> log.warn("cassandra history write failed: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    /**
     * POC: a simple poller scans pending bookings older than the TTL and flips
     * them to cancelled. Cheaper to implement than wiring keyspace notifications,
     * good enough for this POC; production would use a Redis listener or a
     * proper expiry callback.
     */
    @Scheduled(fixedDelay = 15_000L)
    public void expirePendingBookings() {
        Instant cutoff = Instant.now().minus(PENDING_TTL);
        repo.findAll()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isBefore(cutoff))
                .flatMap(b -> {
                    b.setStatus(BookingStatus.CANCELLED);
                    b.setUpdatedAt(Instant.now());
                    return repo.save(b)
                            .then(redis.delete("pending-booking:" + b.getId()))
                            .then(writeHistory(b, BookingHistoryStatus.CANCELED))
                            .doOnSuccess(v -> log.info("expired booking {}", b.getId()));
                })
                .onErrorResume(e -> {
                    log.warn("expire poll failed: {}", e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }

    private BookingResponse toResponse(Booking b) {
        return new BookingResponse(b.getId(), b.getUserId(), b.getHotelId(), b.getRoomId(),
                b.getCheckIn(), b.getCheckOut(), b.getStatus(),
                b.getTotalPrice() == null ? BigDecimal.ZERO : b.getTotalPrice(),
                b.getCreatedAt());
    }
}
