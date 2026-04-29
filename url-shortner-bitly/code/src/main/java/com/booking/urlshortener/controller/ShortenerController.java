package com.booking.urlshortener.controller;

import com.booking.urlshortener.dto.Dtos.ShortenRequest;
import com.booking.urlshortener.dto.Dtos.ShortenResponse;
import com.booking.urlshortener.dto.Dtos.StatsResponse;
import com.booking.urlshortener.dto.Dtos.UrlInfoResponse;
import com.booking.urlshortener.service.AnalyticsService;
import com.booking.urlshortener.service.ShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Tag(name = "Shortener", description = "Create short URLs and inspect their metadata + analytics")
@RestController
@RequestMapping("/api/urls")
public class ShortenerController {

    private final ShortenerService service;
    private final AnalyticsService analytics;

    public ShortenerController(ShortenerService service, AnalyticsService analytics) {
        this.service = service;
        this.analytics = analytics;
    }

    @Operation(summary = "Create a short URL")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ShortenResponse> shorten(ServerHttpRequest req, @Valid @RequestBody ShortenRequest body) {
        String ip = req.getRemoteAddress() == null ? null : req.getRemoteAddress().getAddress().getHostAddress();
        return service.shorten(body, ip);
    }

    @Operation(summary = "Look up metadata for a short code")
    @GetMapping("/{shortCode}")
    public Mono<UrlInfoResponse> info(@PathVariable String shortCode) {
        return service.info(shortCode)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "short code not found")));
    }

    @Operation(summary = "Aggregated click stats for a short code")
    @GetMapping("/{shortCode}/stats")
    public Mono<StatsResponse> stats(@PathVariable String shortCode) {
        return analytics.stats(shortCode);
    }
}
