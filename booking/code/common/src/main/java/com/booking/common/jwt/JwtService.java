package com.booking.common.jwt;

import com.booking.common.security.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * HS256 JWT util used by auth-service to sign tokens and by the API gateway to
 * verify them. Downstream services do NOT use this -- they trust X-User-Id /
 * X-User-Role headers set by the gateway.
 *
 * Every issued token carries a {@code jti} (UUID) so it can be individually
 * revoked via the Redis blocklist.
 */
public class JwtService {

    private final SecretKey key;
    private final long defaultTtlMillis;

    public JwtService(String secret, long defaultTtlMillis) {
        // HS256 requires >= 256-bit key. Pad if too short for dev convenience.
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.defaultTtlMillis = defaultTtlMillis;
    }

    public Issued issue(long userId, UserRole role) {
        return issue(userId, role, defaultTtlMillis);
    }

    public Issued issue(long userId, UserRole role, long ttlMillis) {
        long now = System.currentTimeMillis();
        long exp = now + ttlMillis;
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(key)
                .compact();
        return new Issued(token, jti, exp);
    }

    public Parsed parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
        Claims c = jws.getPayload();
        UserRole role = UserRole.fromOrNull(c.get("role", String.class));
        if (role == null) {
            throw new IllegalArgumentException("unknown role claim in JWT");
        }
        return new Parsed(
                Long.parseLong(c.getSubject()),
                role,
                c.getId(),
                c.getExpiration().getTime());
    }

    /** Result of issuing a token: the encoded JWT, its jti, and absolute exp epoch (ms). */
    public record Issued(String token, String jti, long expEpochMillis) {}

    /** Result of parsing a token. */
    public record Parsed(long userId, UserRole role, String jti, long expEpochMillis) {}
}
