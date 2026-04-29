package com.booking.urlshortener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fires click events into Kafka. Errors are swallowed so a Kafka outage never
 * affects redirect latency or success.
 */
@Component
public class ClickEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventProducer.class);
    public static final String TOPIC = "click-events";

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json = new ObjectMapper();

    public ClickEventProducer(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    public Mono<Void> publish(String shortCode, String userAgent, String ip, String referrer) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("shortCode", shortCode);
            payload.put("clickedAt", Instant.now().toString());
            payload.put("userAgent", userAgent);
            payload.put("ip", ip);
            payload.put("referrer", referrer);
            String body = json.writeValueAsString(payload);
            return Mono.fromFuture(kafka.send(TOPIC, shortCode, body).toCompletableFuture())
                    .doOnError(e -> log.warn("kafka click publish failed: {}", e.getMessage()))
                    .onErrorResume(e -> Mono.empty())
                    .then();
        } catch (JsonProcessingException e) {
            return Mono.empty();
        }
    }
}
