package com.booking.booking.service;

import com.booking.booking.dto.BookingDtos.HotelAvailability;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Calls hotel-service over Eureka-load-balanced WebClient.
 */
@Component
public class HotelClient {

    private final WebClient webClient;

    public HotelClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://hotel-service").build();
    }

    public Mono<HotelAvailability> getAvailability(int hotelId, LocalDate checkIn, LocalDate checkOut) {
        return webClient.get()
                .uri(uri -> uri.path("/api/hotels/{id}/availability")
                        .queryParam("check_in", checkIn)
                        .queryParam("check_out", checkOut)
                        .build(hotelId))
                .retrieve()
                .bodyToMono(RawAvailability.class)
                .map(raw -> new HotelAvailability(raw.hotelId(),
                        raw.rooms().stream()
                                .map(r -> new com.booking.booking.dto.BookingDtos.RoomAvailability(
                                        r.roomId(), r.roomType(), r.pricePerNight(), r.totalPrice(), r.minAvailable()))
                                .toList()));
    }

    public Mono<Void> decrement(int roomId, LocalDate checkIn, LocalDate checkOut) {
        return webClient.post()
                .uri("/internal/availability/decrement")
                .bodyValue(new DecrementBody(roomId, checkIn, checkOut))
                .retrieve()
                .bodyToMono(Void.class);
    }

    private record DecrementBody(@JsonProperty("roomId") int roomId,
                                  @JsonProperty("checkIn") LocalDate checkIn,
                                  @JsonProperty("checkOut") LocalDate checkOut) {}

    private record RawAvailability(@JsonProperty("hotelId") Integer hotelId,
                                    @JsonProperty("rooms") List<RawRoom> rooms) {}

    private record RawRoom(@JsonProperty("roomId") Integer roomId,
                            @JsonProperty("roomType") String roomType,
                            @JsonProperty("pricePerNight") BigDecimal pricePerNight,
                            @JsonProperty("totalPrice") BigDecimal totalPrice,
                            @JsonProperty("minAvailable") Integer minAvailable) {}
}
