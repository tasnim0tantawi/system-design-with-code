package com.booking.urlshortener.cassandra;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@PrimaryKeyClass
public class ClickEventKey implements Serializable {

    @PrimaryKeyColumn(name = "short_code", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String shortCode;

    @PrimaryKeyColumn(name = "clicked_at", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant clickedAt;

    @PrimaryKeyColumn(name = "event_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private UUID eventId;

    public ClickEventKey() {}

    public ClickEventKey(String shortCode, Instant clickedAt, UUID eventId) {
        this.shortCode = shortCode;
        this.clickedAt = clickedAt;
        this.eventId = eventId;
    }

    public String getShortCode() { return shortCode; }
    public Instant getClickedAt() { return clickedAt; }
    public UUID getEventId() { return eventId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClickEventKey k)) return false;
        return Objects.equals(shortCode, k.shortCode)
                && Objects.equals(clickedAt, k.clickedAt)
                && Objects.equals(eventId, k.eventId);
    }

    @Override
    public int hashCode() { return Objects.hash(shortCode, clickedAt, eventId); }
}
