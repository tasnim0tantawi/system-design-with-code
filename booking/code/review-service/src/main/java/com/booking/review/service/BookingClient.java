package com.booking.review.service;

import com.booking.review.dto.ReviewDtos.BookingView;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class BookingClient {

    private final WebClient webClient;

    public BookingClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://booking-service").build();
    }

    public Mono<BookingView> getBooking(int bookingId) {
        return webClient.get()
                .uri("/api/bookings/{id}", bookingId)
                .retrieve()
                .bodyToMono(BookingView.class);
    }
}
