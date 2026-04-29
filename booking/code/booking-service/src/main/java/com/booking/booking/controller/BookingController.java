package com.booking.booking.controller;

import com.booking.booking.cassandra.BookingHistory;
import com.booking.booking.dto.BookingDtos.BookingResponse;
import com.booking.booking.dto.BookingDtos.CreateBookingRequest;
import com.booking.booking.dto.BookingDtos.PaymentResponse;
import com.booking.booking.service.BookingService;
import com.booking.common.security.AuthHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name = "Bookings", description = "Booking lifecycle and payment")
@RestController
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @Operation(summary = "Create a booking for a hotel room")
    @PostMapping("/api/hotels/{hotelId}/bookings")
    public Mono<BookingResponse> create(ServerHttpRequest req, @PathVariable int hotelId,
                                         @Valid @RequestBody CreateBookingRequest body) {
        long uid = AuthHeaders.requireUserId(req);
        return service.create((int) uid, hotelId, body);
    }

    @Operation(summary = "Process payment for a pending booking")
    @PostMapping("/api/bookings/{bookingId}/payment")
    public Mono<PaymentResponse> pay(@PathVariable int bookingId) {
        return service.pay(bookingId);
    }

    @Operation(summary = "Get a booking by ID")
    @GetMapping("/api/bookings/{bookingId}")
    public Mono<BookingResponse> get(@PathVariable int bookingId) {
        return service.get(bookingId);
    }

    @Operation(summary = "List all bookings for a user")
    @GetMapping("/api/users/{userId}/bookings")
    public Flux<BookingResponse> listForUser(@PathVariable int userId) {
        return service.listByUser(userId);
    }

    @Operation(summary = "Cancel a booking")
    @DeleteMapping("/api/bookings/{bookingId}")
    public Mono<BookingResponse> cancel(ServerHttpRequest req, @PathVariable int bookingId) {
        long uid = AuthHeaders.requireUserId(req);
        return service.cancel(bookingId, (int) uid);
    }

    @Operation(summary = "Get booking history for a user (Cassandra)")
    @GetMapping("/api/users/{userId}/booking-history")
    public Flux<BookingHistory> bookingHistory(@PathVariable int userId) {
        return service.historyByUser(userId);
    }
}
