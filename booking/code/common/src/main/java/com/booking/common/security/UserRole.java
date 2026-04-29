package com.booking.common.security;

/**
 * Roles a user can hold. Stored in the database as the enum's name (varchar),
 * carried in JWT claims as the same string, and forwarded by the API gateway
 * as the {@code X-User-Role} header.
 *
 * <p>Single source of truth; no one should ever compare against string literals
 * like {@code "MANAGER"} -- always {@code UserRole.MANAGER} so a typo becomes
 * a compile error.</p>
 */
public enum UserRole {
    /** Guest user; can search hotels, book rooms, leave reviews. */
    USER,
    /** Hotel manager; can create/update hotels and rooms, view bookings. */
    MANAGER,
    /** System administrator; full access (seed-only for now, not enforced). */
    ADMIN;

    /**
     * Lenient parse: case-insensitive, returns null for unknown values
     * (instead of throwing) so callers can decide how to react.
     */
    public static UserRole fromOrNull(String s) {
        if (s == null) return null;
        try {
            return UserRole.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
