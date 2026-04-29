package com.booking.search.service;

import com.booking.search.dto.SearchDtos.SearchResponse;
import com.booking.search.dto.SearchDtos.SearchResultItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SearchService {

    private final MeiliSearchClient meili;
    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper json = new ObjectMapper();

    public SearchService(MeiliSearchClient meili, ReactiveStringRedisTemplate redis) {
        this.meili = meili;
        this.redis = redis;
    }

    public Mono<SearchResponse> search(String location, LocalDate checkIn, LocalDate checkOut,
                                       BigDecimal minPrice, BigDecimal maxPrice, String roomType) {
        String cacheKey = "search:" + canonical(location, checkIn, checkOut, minPrice, maxPrice, roomType);
        return redis.opsForValue().get(cacheKey)
                .flatMap(this::deserialize)
                .switchIfEmpty(
                        meili.search(location, buildFilter(roomType))
                                .map(resp -> {
                                    List<SearchResultItem> items = resp.hits().stream()
                                            .map(h -> new SearchResultItem(h.id(), h.name(), h.location(),
                                                    h.stars(), h.hotelType(), BigDecimal.ZERO))
                                            .toList();
                                    return new SearchResponse(items, items.size());
                                })
                                .flatMap(resp -> serialize(resp)
                                        .flatMap(s -> redis.opsForValue().set(cacheKey, s, Duration.ofSeconds(60)))
                                        .thenReturn(resp))
                );
    }

    private String buildFilter(String roomType) {
        List<String> clauses = new ArrayList<>();
        clauses.add("status = \"verified\"");
        if (roomType != null && !roomType.isBlank()) {
            clauses.add("hotelType = \"" + roomType.replace("\"", "") + "\"");
        }
        return String.join(" AND ", clauses);
    }

    private String canonical(String location, LocalDate ci, LocalDate co, BigDecimal min, BigDecimal max, String rt) {
        return Optional.ofNullable(location).orElse("") + "|"
                + Optional.ofNullable(ci).map(LocalDate::toString).orElse("") + "|"
                + Optional.ofNullable(co).map(LocalDate::toString).orElse("") + "|"
                + Optional.ofNullable(min).map(BigDecimal::toPlainString).orElse("") + "|"
                + Optional.ofNullable(max).map(BigDecimal::toPlainString).orElse("") + "|"
                + Optional.ofNullable(rt).orElse("");
    }

    private Mono<String> serialize(SearchResponse r) {
        try { return Mono.just(json.writeValueAsString(r)); }
        catch (Exception e) { return Mono.error(e); }
    }

    private Mono<SearchResponse> deserialize(String s) {
        try { return Mono.just(json.readValue(s, new TypeReference<SearchResponse>() {})); }
        catch (Exception e) { return Mono.empty(); }
    }
}
