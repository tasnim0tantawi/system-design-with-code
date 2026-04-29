package com.booking.auth.service;

import com.booking.common.jwt.TokenKeys;
import com.booking.common.security.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * All Redis-backed token state lives here.
 *
 * <ul>
 *   <li>Refresh tokens: 32 random bytes, URL-safe base64. Stored at
 *       {@code refresh:{token}} with value {@code userId|role} and TTL =
 *       refresh-ttl-millis. We never store hashes because the value itself is
 *       already high-entropy and only ever moves over TLS; the only attack is
 *       Redis compromise, in which case a hash wouldn't save us either since
 *       the attacker also gets the user table.</li>
 *   <li>Access token blocklist: a key {@code blocklist:{jti}} with any value
 *       and TTL equal to the token's remaining lifetime. The TTL bound means
 *       the blocklist set never grows unbounded.</li>
 * </ul>
 */
@Component
public class TokenStore {

    private static final Logger log = LoggerFactory.getLogger(TokenStore.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final ReactiveStringRedisTemplate redis;

    public TokenStore(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    public Mono<String> issueRefreshToken(long userId, UserRole role, Duration ttl) {
        String token = newOpaqueToken();
        String value = userId + "|" + role.name();
        return redis.opsForValue().set(TokenKeys.refresh(token), value, ttl)
                .thenReturn(token);
    }

    /**
     * Look up the refresh token AND delete it in one round trip (rotation).
     * Returns the parsed user info or empty if the token isn't present / has
     * already been used.
     */
    public Mono<RefreshClaims> consumeRefreshToken(String token) {
        String key = TokenKeys.refresh(token);
        return redis.opsForValue().getAndDelete(key)
                .map(this::parse);
    }

    public Mono<Void> revokeRefreshToken(String token) {
        if (token == null || token.isBlank()) return Mono.empty();
        return redis.delete(TokenKeys.refresh(token)).then();
    }

    /**
     * Add the access token jti to the blocklist with a TTL equal to its
     * remaining lifetime. After natural expiry the entry self-evicts.
     */
    public Mono<Void> blocklistAccessToken(String jti, long expEpochMillis) {
        long remainingMs = expEpochMillis - Instant.now().toEpochMilli();
        if (remainingMs <= 0) {
            return Mono.empty();  // already expired -- no need to blocklist
        }
        return redis.opsForValue().set(TokenKeys.blocklist(jti), "1", Duration.ofMillis(remainingMs))
                .then();
    }

    private String newOpaqueToken() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private RefreshClaims parse(String value) {
        int sep = value.indexOf('|');
        if (sep < 0) {
            log.warn("malformed refresh value in Redis: {}", value);
            return null;
        }
        long userId = Long.parseLong(value.substring(0, sep));
        UserRole role = UserRole.fromOrNull(value.substring(sep + 1));
        if (role == null) {
            log.warn("unknown role in refresh value: {}", value);
            return null;
        }
        return new RefreshClaims(userId, role);
    }

    public record RefreshClaims(long userId, UserRole role) {}
}
