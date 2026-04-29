package com.booking.gateway.filter;

import com.booking.common.jwt.JwtService;
import com.booking.common.jwt.TokenKeys;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Validates the Authorization: Bearer JWT on every request EXCEPT
 * /api/auth/**, swagger, and public hotel browsing (GET /api/hotels/**, except
 * the manager-only /api/hotels/{id}/bookings).
 *
 * On success: checks the Redis blocklist for the token's jti (set on logout).
 * If not blocklisted, strips the Authorization header and forwards X-User-Id
 * / X-User-Role to downstream services.
 */
@Component
public class JwtAuthFilter implements WebFilter, Ordered {

    private final JwtService jwt;
    private final ReactiveStringRedisTemplate redis;

    public JwtAuthFilter(JwtService jwt, ReactiveStringRedisTemplate redis) {
        this.jwt = jwt;
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getURI().getPath();

        if (isPublic(req.getMethod(), path)) {
            return chain.filter(exchange);
        }

        String auth = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = auth.substring(7);
        JwtService.Parsed parsed;
        try {
            parsed = jwt.parse(token);
        } catch (Exception e) {
            return unauthorized(exchange);
        }

        return redis.hasKey(TokenKeys.blocklist(parsed.jti()))
                .flatMap(blocked -> {
                    if (Boolean.TRUE.equals(blocked)) {
                        return unauthorized(exchange);
                    }
                    ServerHttpRequest mutated = req.mutate()
                            .headers(h -> {
                                h.remove(HttpHeaders.AUTHORIZATION);
                                h.set("X-User-Id", String.valueOf(parsed.userId()));
                                h.set("X-User-Role", parsed.role().name());
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private boolean isPublic(HttpMethod method, String path) {
        if (path.startsWith("/api/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")) {
            return true;
        }
        // Public hotel browsing: GET /api/hotels, /api/hotels/{id}, /api/hotels/search/**,
        // /api/hotels/{id}/availability, /api/hotels/{id}/reviews. Bookings under a hotel
        // (/api/hotels/{id}/bookings) stay protected -- they're manager-only.
        if (HttpMethod.GET.equals(method)
                && path.startsWith("/api/hotels")
                && !path.contains("/bookings")) {
            return true;
        }
        return false;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
