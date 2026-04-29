package com.booking.auth.service;

import com.booking.auth.dto.AuthDtos.LoginRequest;
import com.booking.auth.dto.AuthDtos.RefreshRequest;
import com.booking.auth.dto.AuthDtos.RegisterRequest;
import com.booking.auth.dto.AuthDtos.TokenResponse;
import com.booking.auth.entity.AuthUser;
import com.booking.auth.repository.AuthUserRepository;
import com.booking.common.exception.ApiException;
import com.booking.common.jwt.JwtService;
import com.booking.common.security.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    private final AuthUserRepository repo;
    private final PasswordHasher hasher;
    private final JwtService jwt;
    private final TokenStore tokens;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public AuthService(AuthUserRepository repo, PasswordHasher hasher, JwtService jwt, TokenStore tokens,
                       @Value("${jwt.access-ttl-millis:900000}") long accessTtlMs,
                       @Value("${jwt.refresh-ttl-millis:604800000}") long refreshTtlMs) {
        this.repo = repo;
        this.hasher = hasher;
        this.jwt = jwt;
        this.tokens = tokens;
        this.accessTtl = Duration.ofMillis(accessTtlMs);
        this.refreshTtl = Duration.ofMillis(refreshTtlMs);
    }

    public Mono<TokenResponse> register(RegisterRequest req, UserRole role) {
        return repo.existsByEmail(req.email())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(ApiException.conflict("email already registered"));
                    }
                    AuthUser user = AuthUser.builder()
                            .email(req.email())
                            .passwordHash(hasher.hash(req.password()))
                            .name(req.name())
                            .role(role)
                            .createdAt(Instant.now())
                            .build();
                    return repo.save(user);
                })
                .flatMap(this::issuePair);
    }

    public Mono<TokenResponse> login(LoginRequest req) {
        return repo.findByEmail(req.email())
                .switchIfEmpty(Mono.error(ApiException.forbidden("invalid credentials")))
                .flatMap(u -> {
                    if (!hasher.matches(req.password(), u.getPasswordHash())) {
                        return Mono.error(ApiException.forbidden("invalid credentials"));
                    }
                    return issuePair(u);
                });
    }

    /**
     * Token rotation: the old refresh token is consumed (deleted) and a new
     * pair is issued. If a stolen refresh token is used, the legitimate user's
     * next refresh attempt fails -- a clear signal to force re-login.
     */
    public Mono<TokenResponse> refresh(RefreshRequest req) {
        return tokens.consumeRefreshToken(req.refreshToken())
                .switchIfEmpty(Mono.error(ApiException.forbidden("invalid or expired refresh token")))
                .flatMap(claims -> issuePair(claims.userId(), claims.role()));
    }

    /**
     * Revokes the access token (blocklist its jti) and the refresh token (delete from Redis).
     * Idempotent: if the access token is already expired or the refresh token is unknown,
     * still returns success.
     */
    public Mono<Void> logout(String accessToken, String refreshToken) {
        Mono<Void> revokeAccess = Mono.empty();
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                JwtService.Parsed parsed = jwt.parse(accessToken);
                revokeAccess = tokens.blocklistAccessToken(parsed.jti(), parsed.expEpochMillis());
            } catch (Exception e) {
                // Bad/expired token -- nothing to revoke. Don't leak which.
            }
        }
        return revokeAccess.then(tokens.revokeRefreshToken(refreshToken));
    }

    // --- helpers ---

    private Mono<TokenResponse> issuePair(AuthUser u) {
        return issuePair(u.getId(), u.getRole());
    }

    private Mono<TokenResponse> issuePair(long userId, UserRole role) {
        JwtService.Issued access = jwt.issue(userId, role, accessTtl.toMillis());
        return tokens.issueRefreshToken(userId, role, refreshTtl)
                .map(refresh -> new TokenResponse(
                        "Bearer",
                        access.token(),
                        accessTtl.toSeconds(),
                        refresh,
                        refreshTtl.toSeconds(),
                        userId,
                        role));
    }
}
