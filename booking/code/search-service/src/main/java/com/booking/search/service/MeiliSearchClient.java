package com.booking.search.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class MeiliSearchClient {

    private static final Logger log = LoggerFactory.getLogger(MeiliSearchClient.class);
    private static final String INDEX = "hotels";

    private final WebClient webClient;
    private final ObjectMapper json = new ObjectMapper();

    public MeiliSearchClient(@Value("${meilisearch.url:http://localhost:7700}") String url) {
        this.webClient = WebClient.builder().baseUrl(url).build();
    }

    @PostConstruct
    public void initIndex() {
        webClient.patch()
            .uri("/indexes/{idx}/settings", INDEX)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "searchableAttributes": ["name", "location"],
                  "filterableAttributes": ["stars", "hotelType", "status"]
                }
                """)
            .retrieve()
            .bodyToMono(String.class)
            .doOnSuccess(r -> log.info("MeiliSearch index '{}' settings applied", INDEX))
            .onErrorResume(e -> { log.warn("MeiliSearch settings init failed: {}", e.getMessage()); return Mono.empty(); })
            .subscribe();
    }

    public Mono<Void> upsert(HotelDocument doc) {
        try {
            String body = json.writeValueAsString(List.of(doc));
            return webClient.post()
                .uri("/indexes/{idx}/documents?primaryKey=id", INDEX)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(r -> log.debug("upserted hotel id={} in MeiliSearch", doc.id()))
                .onErrorResume(e -> { log.warn("MeiliSearch upsert failed: {}", e.getMessage()); return Mono.empty(); })
                .then();
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    public Mono<Void> delete(int hotelId) {
        return webClient.delete()
            .uri("/indexes/{idx}/documents/{id}", INDEX, hotelId)
            .retrieve()
            .bodyToMono(String.class)
            .doOnSuccess(r -> log.debug("deleted hotel id={} from MeiliSearch", hotelId))
            .onErrorResume(e -> { log.warn("MeiliSearch delete failed: {}", e.getMessage()); return Mono.empty(); })
            .then();
    }

    public Mono<MeiliSearchResponse> search(String q, String filter) {
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("q", q != null ? q : "");
            if (filter != null && !filter.isBlank()) body.put("filter", filter);
            body.put("limit", 100);
            String bodyJson = json.writeValueAsString(body);
            return webClient.post()
                .uri("/indexes/{idx}/search", INDEX)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyJson)
                .retrieve()
                .bodyToMono(MeiliSearchResponse.class)
                .onErrorResume(e -> { log.warn("MeiliSearch search failed: {}", e.getMessage()); return Mono.just(new MeiliSearchResponse(List.of())); });
        } catch (Exception e) {
            return Mono.just(new MeiliSearchResponse(List.of()));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HotelDocument(int id, String name, String location, int stars, String hotelType, String status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeiliSearchResponse(List<HotelDocument> hits) {}
}
