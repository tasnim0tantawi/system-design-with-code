package com.booking.hotel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class HotelDtos {

    public record CreateHotelRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 4000) String description,
            @NotBlank @Size(max = 255) String location,
            @NotNull @Min(1) @Max(5) Integer stars,
            @NotBlank @Size(max = 64) String type,
            @NotBlank String businessCredentials
    ) {}

    public record HotelResponse(Integer id, Integer managerId, String name, String description,
                                String location, Integer stars, String type, String status) {}

    public record CreateRoomRequest(
            @NotBlank @Size(max = 32) String roomType,
            @NotNull @DecimalMin(value = "0.01", message = "basePrice must be greater than 0") BigDecimal basePrice,
            @NotNull @Min(1) Integer totalCount
    ) {}

    public record RoomResponse(Integer id, Integer hotelId, String roomType, BigDecimal basePrice,
                               Integer totalCount) {}

    public record AvailabilityResponse(Integer hotelId, LocalDate checkIn, LocalDate checkOut,
                                       List<RoomAvailabilityItem> rooms) {}

    public record RoomAvailabilityItem(Integer roomId, String roomType, BigDecimal pricePerNight,
                                       BigDecimal totalPrice, Integer minAvailable) {}

    public record DecrementRequest(
            @NotNull @Positive Integer roomId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut
    ) {}

    public record PresignResponse(String key, String publicUrl, String uploadUrl) {}

    private HotelDtos() {}
}
