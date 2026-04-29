package com.booking.common.dto;

import java.util.List;

/**
 * Cursor-based pagination response. Stateless and stable under concurrent inserts:
 * the cursor encodes the last seen sort key, so the next page resumes from there
 * regardless of how many rows were inserted in between.
 *
 * @param items      the page of items
 * @param nextCursor opaque cursor to pass back as ?cursor=... for the next page;
 *                   null when {@code hasMore} is false
 * @param hasMore    whether more items exist beyond this page
 */
public record CursorPageResponse<T>(List<T> items, String nextCursor, boolean hasMore) {
    public static <T> CursorPageResponse<T> of(List<T> items, String nextCursor, boolean hasMore) {
        return new CursorPageResponse<>(items, nextCursor, hasMore);
    }
}
