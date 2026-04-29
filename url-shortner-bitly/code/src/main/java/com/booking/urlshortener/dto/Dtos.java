package com.booking.urlshortener.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class Dtos {

    public record ShortenRequest(
            @NotBlank
            @Size(max = 4096)
            @Pattern(regexp = "^https?://.+", message = "must be an http(s) URL")
            String longUrl,

            @Min(1)
            Integer ttlHours
    ) {}

    public record ShortenResponse(
            String shortCode,
            String shortUrl,
            String longUrl,
            Instant createdAt,
            Instant expiresAt
    ) {}

    public record UrlInfoResponse(
            String shortCode,
            String longUrl,
            Instant createdAt,
            Instant expiresAt,
            String creatorIp
    ) {}

    public record DailyCount(String day, long count) {}

    public record StatsResponse(
            String shortCode,
            long totalClicks,
            List<DailyCount> dailyCounts
    ) {}

    private Dtos() {}
}
