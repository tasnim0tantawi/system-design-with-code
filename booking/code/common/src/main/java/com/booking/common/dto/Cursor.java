package com.booking.common.dto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Opaque ID-based cursor encoded as URL-safe Base64. Treats the cursor as
 * "give me items with id strictly greater than this". A null/blank cursor means
 * "start from the beginning" and is decoded as 0.
 *
 * The encoding is intentionally opaque so clients don't reason about its
 * internals -- this lets us change the cursor format later (e.g., to
 * (timestamp, id) tuples) without breaking existing clients.
 */
public final class Cursor {

    private Cursor() {}

    public static String encode(long id) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Long.toString(id).getBytes(StandardCharsets.UTF_8));
    }

    public static long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0L;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            return Long.parseLong(decoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid cursor: " + cursor);
        }
    }
}
