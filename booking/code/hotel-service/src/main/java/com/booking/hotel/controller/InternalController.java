package com.booking.hotel.controller;

import com.booking.hotel.dto.HotelDtos.DecrementRequest;
import com.booking.hotel.service.HotelService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Hidden
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final HotelService service;

    public InternalController(HotelService service) {
        this.service = service;
    }

    @PostMapping("/availability/decrement")
    public Mono<Void> decrement(@Valid @RequestBody DecrementRequest req) {
        return service.decrementAvailability(req);
    }
}
