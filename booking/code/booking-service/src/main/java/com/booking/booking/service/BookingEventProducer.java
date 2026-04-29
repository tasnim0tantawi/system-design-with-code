package com.booking.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class BookingEventProducer {

    private static final Logger log = LoggerFactory.getLogger(BookingEventProducer.class);
    private static final String TOPIC = "booking-events";

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json = new ObjectMapper();

    public BookingEventProducer(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    public Mono<Void> sendConfirmed(int userId, int bookingId) {
        return send(userId, "booking_confirmed", "Booking #" + bookingId + " confirmed");
    }

    private Mono<Void> send(int userId, String type, String message) {
        try {
            String payload = json.writeValueAsString(Map.of(
                "userId", userId, "type", type, "message", message));
            return Mono.fromFuture(kafka.send(TOPIC, String.valueOf(userId), payload).toCompletableFuture())
                    .doOnSuccess(r -> log.debug("kafka sent {} to {}", type, TOPIC))
                    .onErrorResume(e -> { log.warn("kafka send failed: {}", e.getMessage()); return Mono.empty(); })
                    .then();
        } catch (JsonProcessingException e) {
            return Mono.empty();
        }
    }
}
