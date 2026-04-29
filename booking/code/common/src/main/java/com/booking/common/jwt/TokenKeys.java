package com.booking.common.jwt;

/**
 * Shared Redis key conventions for JWT lifecycle.
 *
 * <ul>
 *   <li>{@code blocklist:{jti}} -- present until the access token's natural expiry.
 *       Set by auth-service on logout, checked by the gateway on every request.</li>
 *   <li>{@code refresh:{token}} -- maps an opaque refresh token to {@code userId|role}.
 *       Set on login/refresh, deleted on refresh (rotation) or logout.</li>
 * </ul>
 */
public final class TokenKeys {
    private TokenKeys() {}

    public static String blocklist(String jti) {
        return "blocklist:" + jti;
    }

    public static String refresh(String token) {
        return "refresh:" + token;
    }
}
