package com.booking.hotel.service;

import com.booking.hotel.dto.HotelDtos.HotelResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HotelEventProducer {

    private static final Logger log = LoggerFactory.getLogger(HotelEventProducer.class);
    private static final String TOPIC = "hotel-events";

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json = new ObjectMapper();

    public HotelEventProducer(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    public Mono<Void> sendCreated(HotelResponse hotel) {
        return send("hotel.created", hotel);
    }

    public Mono<Void> sendUpdated(HotelResponse hotel) {
        return send("hotel.updated", hotel);
    }

    public Mono<Void> sendDeleted(int hotelId) {
        try {
            String payload = json.writeValueAsString(Map.of("type", "hotel.deleted", "id", hotelId));
            return Mono.fromFuture(kafka.send(TOPIC, String.valueOf(hotelId), payload).toCompletableFuture())
                    .doOnSuccess(r -> log.debug("kafka sent hotel.deleted for id={}", hotelId))
                    .onErrorResume(e -> { log.warn("kafka send failed: {}", e.getMessage()); return Mono.empty(); })
                    .then();
        } catch (JsonProcessingException e) {
            return Mono.empty();
        }
    }

    private Mono<Void> send(String type, HotelResponse hotel) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", type);
            payload.put("id", hotel.id());
            payload.put("name", hotel.name());
            payload.put("location", hotel.location());
            payload.put("stars", hotel.stars());
            payload.put("hotelType", hotel.type());
            payload.put("status", hotel.status());
            String body = json.writeValueAsString(payload);
            return Mono.fromFuture(kafka.send(TOPIC, String.valueOf(hotel.id()), body).toCompletableFuture())
                    .doOnSuccess(r -> log.debug("kafka sent {} for hotel id={}", type, hotel.id()))
                    .onErrorResume(e -> { log.warn("kafka send failed: {}", e.getMessage()); return Mono.empty(); })
                    .then();
        } catch (JsonProcessingException e) {
            return Mono.empty();
        }
    }
}
