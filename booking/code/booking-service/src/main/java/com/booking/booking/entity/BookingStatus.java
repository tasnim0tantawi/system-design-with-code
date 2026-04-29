package com.booking.booking.entity;

/**
 * Lifecycle states of a booking row in the PostgreSQL {@code booking} table.
 *
 * <p>Transitions:</p>
 * <pre>
 *   create -> PENDING
 *
 *   PENDING --pay (success)--> CONFIRMED
 *   PENDING --pay (failure)--> CANCELLED
 *   PENDING --TTL expiry-->     CANCELLED
 *   PENDING --user cancel-->    CANCELLED
 *
 *   CONFIRMED --user cancel-->  CANCELLED
 *   CONFIRMED --stay over-->    COMPLETED
 * </pre>
 *
 * <p>The Cassandra {@code booking_history} audit log only records terminal
 * states ({@link com.booking.booking.cassandra.BookingHistoryStatus#COMPLETED}
 * and {@code CANCELED}). Active intermediate states (PENDING, CONFIRMED) live
 * only here in PostgreSQL.</p>
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
