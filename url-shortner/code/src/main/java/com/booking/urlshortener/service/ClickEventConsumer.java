package com.booking.urlshortener.service;

import com.booking.urlshortener.cassandra.ClickEvent;
import com.booking.urlshortener.cassandra.ClickEventKey;
import com.booking.urlshortener.cassandra.ClickEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Consumes click events from Kafka and writes them to Cassandra.
 *
 * <ul>
 *   <li>{@code click_events} -- full event row, partitioned by short_code,
 *       clustered by clicked_at DESC. Source of truth.</li>
 *   <li>{@code click_counts} -- counter table with one row per (short_code,
 *       day). Lets analytics queries read aggregated totals in O(1).</li>
 * </ul>
 */
@Component
public class ClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    private final ClickEventRepository repo;
    private final ReactiveCassandraTemplate cassandra;
    private final ObjectMapper json = new ObjectMapper();

    public ClickEventConsumer(ClickEventRepository repo, ReactiveCassandraTemplate cassandra) {
        this.repo = repo;
        this.cassandra = cassandra;
    }

    @KafkaListener(topics = ClickEventProducer.TOPIC, groupId = "url-shortener-analytics")
    public void onClick(String message) {
        try {
            JsonNode n = json.readTree(message);
            String shortCode = n.get("shortCode").asText();
            Instant clickedAt = Instant.parse(n.get("clickedAt").asText());
            UUID eventId = UUID.fromString(n.get("eventId").asText());

            ClickEvent ev = new ClickEvent();
            ev.setKey(new ClickEventKey(shortCode, clickedAt, eventId));
            ev.setUserAgent(textOrNull(n, "userAgent"));
            ev.setIp(textOrNull(n, "ip"));
            ev.setReferrer(textOrNull(n, "referrer"));

            String day = LocalDate.ofInstant(clickedAt, ZoneOffset.UTC).toString();

            repo.save(ev)
                .then(cassandra.getReactiveCqlOperations().execute(
                    "UPDATE click_counts SET count = count + 1 WHERE short_code = ? AND bucket_day = ?",
                    shortCode, day))
                .doOnError(e -> log.error("failed to persist click event: {}", e.getMessage()))
                .subscribe();
        } catch (Exception e) {
            log.error("malformed click event: {}", e.getMessage());
        }
    }

    private static String textOrNull(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
