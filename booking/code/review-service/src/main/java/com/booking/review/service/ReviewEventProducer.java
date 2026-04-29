package com.booking.review.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class ReviewEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ReviewEventProducer.class);
    private static final String TOPIC = "review-events";

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json = new ObjectMapper();

    public ReviewEventProducer(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    public Mono<Void> sendReviewCreated(int userId, int reviewId) {
        return send(userId, "review_created", "Review #" + reviewId + " posted");
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
