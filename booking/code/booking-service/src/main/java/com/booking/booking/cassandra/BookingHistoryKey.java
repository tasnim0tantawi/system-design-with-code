package com.booking.booking.cassandra;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@PrimaryKeyClass
public class BookingHistoryKey implements Serializable {

    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private int userId;

    @PrimaryKeyColumn(name = "created_at", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant createdAt;

    @PrimaryKeyColumn(name = "booking_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private int bookingId;

    public BookingHistoryKey() {}

    public BookingHistoryKey(int userId, Instant createdAt, int bookingId) {
        this.userId = userId;
        this.createdAt = createdAt;
        this.bookingId = bookingId;
    }

    public int getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
    public int getBookingId() { return bookingId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingHistoryKey k)) return false;
        return userId == k.userId && bookingId == k.bookingId && Objects.equals(createdAt, k.createdAt);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, createdAt, bookingId); }
}
