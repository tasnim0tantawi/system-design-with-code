package com.booking.common.security;

import com.booking.common.exception.ApiException;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Reads the trusted X-User-Id / X-User-Role headers set by the API Gateway
 * after JWT validation. Downstream services rely on these headers exclusively;
 * external traffic always passes through the gateway, so spoofing is not a concern.
 */
public final class AuthHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";

    private AuthHeaders() {}

    public static long requireUserId(ServerHttpRequest req) {
        String v = req.getHeaders().getFirst(USER_ID);
        if (v == null || v.isBlank()) {
            throw ApiException.forbidden("missing " + USER_ID);
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw ApiException.forbidden("invalid " + USER_ID);
        }
    }

    public static UserRole requireRole(ServerHttpRequest req) {
        String v = req.getHeaders().getFirst(USER_ROLE);
        if (v == null || v.isBlank()) {
            throw ApiException.forbidden("missing " + USER_ROLE);
        }
        UserRole role = UserRole.fromOrNull(v);
        if (role == null) {
            throw ApiException.forbidden("unknown role: " + v);
        }
        return role;
    }

    public static void requireRole(ServerHttpRequest req, UserRole expected) {
        UserRole role = requireRole(req);
        if (role != expected) {
            throw ApiException.forbidden("requires role " + expected);
        }
    }
}
