package com.booking.hotel.controller;

import com.booking.common.dto.CursorPageResponse;
import com.booking.common.security.AuthHeaders;
import com.booking.common.security.UserRole;
import com.booking.hotel.dto.HotelDtos.*;
import com.booking.hotel.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "Hotels", description = "Hotel and room management")
@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService service;

    public HotelController(HotelService service) {
        this.service = service;
    }

    @Operation(summary = "Create a hotel (MANAGER only)")
    @PostMapping
    public Mono<HotelResponse> create(ServerHttpRequest req, @Valid @RequestBody CreateHotelRequest body) {
        AuthHeaders.requireRole(req, UserRole.MANAGER);
        return service.createHotel(AuthHeaders.requireUserId(req), body);
    }

    @Operation(summary = "List hotels (cursor-paginated)")
    @GetMapping
    public Mono<CursorPageResponse<HotelResponse>> listAll(
            @Parameter(description = "Opaque cursor returned by the previous page; omit for first page")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "Max items per page (1-100, default 20)")
            @RequestParam(defaultValue = "20") int limit) {
        return service.listPage(cursor, limit);
    }

    @Operation(summary = "Get a hotel by ID")
    @GetMapping("/{hotelId}")
    public Mono<HotelResponse> get(@PathVariable int hotelId) {
        return service.getHotel(hotelId);
    }

    @Operation(summary = "Update hotel details (MANAGER only, own hotels)")
    @PutMapping("/{hotelId}")
    public Mono<HotelResponse> update(ServerHttpRequest req, @PathVariable int hotelId,
                                       @Valid @RequestBody CreateHotelRequest body) {
        AuthHeaders.requireRole(req, UserRole.MANAGER);
        return service.updateHotel(hotelId, AuthHeaders.requireUserId(req), body);
    }

    @Operation(summary = "Add a room type to a hotel (MANAGER only)")
    @PostMapping("/{hotelId}/rooms")
    public Mono<RoomResponse> addRoom(ServerHttpRequest req, @PathVariable int hotelId,
                                       @Valid @RequestBody CreateRoomRequest body) {
        AuthHeaders.requireRole(req, UserRole.MANAGER);
        return service.addRoom(hotelId, AuthHeaders.requireUserId(req), body);
    }

    @Operation(summary = "Check room availability for date range")
    @GetMapping("/{hotelId}/availability")
    public Mono<AvailabilityResponse> availability(@PathVariable int hotelId,
                                                    @RequestParam("check_in") String checkIn,
                                                    @RequestParam("check_out") String checkOut) {
        return service.getAvailability(hotelId, LocalDate.parse(checkIn), LocalDate.parse(checkOut));
    }

    @Operation(summary = "Get a pre-signed URL to upload a hotel image")
    @PostMapping("/images/presign")
    public Mono<PresignResponse> presign(@RequestBody(required = false) Map<String, String> body) {
        String contentType = body == null ? "image/jpeg" : body.getOrDefault("contentType", "image/jpeg");
        return service.presign(contentType);
    }
}
