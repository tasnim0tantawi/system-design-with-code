package com.booking.booking.cassandra;

/**
 * Terminal states a booking can land in. Only these go to the Cassandra
 * {@code booking_history} table -- the table is an append-only audit log of
 * "what eventually happened" to each booking, not the live lifecycle.
 *
 * <p>Active intermediate states (PENDING, CONFIRMED) live in the PostgreSQL
 * {@code booking} table and never appear here.</p>
 */
public enum BookingHistoryStatus {
    /** The booking was paid and (eventually) fulfilled. */
    COMPLETED,
    /** The booking was cancelled by the user, by payment failure, or by an admin. */
    CANCELED
}
