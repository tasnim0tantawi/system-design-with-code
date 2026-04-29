package com.booking.booking.dto;

import com.booking.booking.entity.BookingStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class BookingDtos {

    public record CreateBookingRequest(
            @NotNull @Positive Integer roomId,
            @NotNull @FutureOrPresent LocalDate checkIn,
            @NotNull @Future LocalDate checkOut
    ) {}

    public record BookingResponse(Integer id, Integer userId, Integer hotelId, Integer roomId,
                                   LocalDate checkIn, LocalDate checkOut, BookingStatus status,
                                   BigDecimal totalPrice, Instant createdAt) {}

    public record PaymentResponse(Integer bookingId, boolean success, BookingStatus status, String chargeId,
                                  String failureReason) {}

    /** Subset of hotel-service AvailabilityResponse we need. */
    public record HotelAvailability(Integer hotelId, java.util.List<RoomAvailability> rooms) {}

    public record RoomAvailability(Integer roomId, String roomType, BigDecimal pricePerNight,
                                    BigDecimal totalPrice, Integer minAvailable) {}

    private BookingDtos() {}
}
