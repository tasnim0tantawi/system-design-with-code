package com.booking.urlshortener.service;

import com.booking.urlshortener.dto.Dtos.ShortenRequest;
import com.booking.urlshortener.dto.Dtos.ShortenResponse;
import com.booking.urlshortener.dto.Dtos.UrlInfoResponse;
import com.booking.urlshortener.entity.UrlMapping;
import com.booking.urlshortener.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Service
public class ShortenerService {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final UrlMappingRepository repo;
    private final TokenRangeAllocator allocator;
    private final ReactiveStringRedisTemplate redis;
    private final String baseUrl;
    private final int defaultTtlHours;

    public ShortenerService(UrlMappingRepository repo, TokenRangeAllocator allocator,
                            ReactiveStringRedisTemplate redis,
                            @Value("${shortener.base-url}") String baseUrl,
                            @Value("${shortener.default-ttl-hours:8760}") int defaultTtlHours) {
        this.repo = repo;
        this.allocator = allocator;
        this.redis = redis;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.defaultTtlHours = defaultTtlHours;
    }

    public Mono<ShortenResponse> shorten(ShortenRequest req, String creatorIp) {
        int ttl = req.ttlHours() == null ? defaultTtlHours : req.ttlHours();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofHours(ttl));

        return allocator.next()
                .map(Base62::encode)
                .flatMap(code -> {
                    UrlMapping m = new UrlMapping();
                    m.setShortCode(code);
                    m.setLongUrl(req.longUrl());
                    m.setCreatedAt(now);
                    m.setExpiresAt(expiresAt);
                    m.setCreatorIp(creatorIp);
                    return repo.save(m);
                })
                .flatMap(saved -> redis.opsForValue()
                        .set(cacheKey(saved.getShortCode()), saved.getLongUrl(), CACHE_TTL)
                        .thenReturn(saved))
                .map(saved -> new ShortenResponse(
                        saved.getShortCode(),
                        baseUrl + "/" + saved.getShortCode(),
                        saved.getLongUrl(),
                        saved.getCreatedAt(),
                        saved.getExpiresAt()));
    }

    /**
     * Resolves a short code to its long URL using a Redis-cached read.
     * Returns empty if the code is unknown or expired.
     */
    public Mono<String> resolve(String shortCode) {
        String key = cacheKey(shortCode);
        return redis.opsForValue().get(key)
                .switchIfEmpty(repo.findById(shortCode)
                        .filter(m -> m.getExpiresAt().isAfter(Instant.now()))
                        .flatMap(m -> redis.opsForValue()
                                .set(key, m.getLongUrl(), CACHE_TTL)
                                .thenReturn(m.getLongUrl())));
    }

    public Mono<UrlInfoResponse> info(String shortCode) {
        return repo.findById(shortCode)
                .map(m -> new UrlInfoResponse(
                        m.getShortCode(),
                        m.getLongUrl(),
                        m.getCreatedAt(),
                        m.getExpiresAt(),
                        m.getCreatorIp()));
    }

    private String cacheKey(String shortCode) {
        return "url:" + shortCode;
    }
}
