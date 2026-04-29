package com.booking.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class HotelIndexer {

    private static final Logger log = LoggerFactory.getLogger(HotelIndexer.class);

    private final MeiliSearchClient meili;
    private final ObjectMapper json = new ObjectMapper();

    public HotelIndexer(MeiliSearchClient meili) {
        this.meili = meili;
    }

    @KafkaListener(topics = "hotel-events", groupId = "search-service")
    public void onHotelEvent(String message) {
        try {
            JsonNode node = json.readTree(message);
            String type = node.get("type").asText();
            int id = node.get("id").asInt();

            switch (type) {
                case "hotel.created", "hotel.updated" -> {
                    var doc = new MeiliSearchClient.HotelDocument(
                        id,
                        node.path("name").asText(null),
                        node.path("location").asText(null),
                        node.path("stars").asInt(0),
                        node.path("hotelType").asText(null),
                        node.path("status").asText(null)
                    );
                    meili.upsert(doc).subscribe();
                }
                case "hotel.deleted" -> meili.delete(id).subscribe();
                default -> log.warn("unknown hotel event type: {}", type);
            }
        } catch (Exception e) {
            log.error("failed to process hotel event: {}", e.getMessage());
        }
    }
}
