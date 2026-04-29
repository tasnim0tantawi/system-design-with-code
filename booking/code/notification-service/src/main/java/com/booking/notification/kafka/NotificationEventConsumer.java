package com.booking.notification.kafka;

import com.booking.notification.dto.NotificationDtos.InternalRequest;
import com.booking.notification.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationService service;
    private final ObjectMapper json = new ObjectMapper();

    public NotificationEventConsumer(NotificationService service) {
        this.service = service;
    }

    @KafkaListener(topics = "booking-events", groupId = "notification-service")
    public void onBookingEvent(String message) {
        handle(message);
    }

    @KafkaListener(topics = "review-events", groupId = "notification-service")
    public void onReviewEvent(String message) {
        handle(message);
    }

    private void handle(String message) {
        try {
            JsonNode node = json.readTree(message);
            int userId = node.get("userId").asInt();
            String type = node.get("type").asText();
            String msg = node.get("message").asText();
            service.create(new InternalRequest(userId, type, msg))
                    .subscribe(
                        n -> log.debug("notification created id={}", n.id()),
                        e -> log.warn("notification create failed: {}", e.getMessage())
                    );
        } catch (Exception e) {
            log.warn("failed to parse kafka message: {}", e.getMessage());
        }
    }
}
