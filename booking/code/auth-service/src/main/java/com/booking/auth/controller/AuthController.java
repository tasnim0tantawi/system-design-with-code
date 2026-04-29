package com.booking.auth.controller;

import com.booking.auth.dto.AuthDtos.LoginRequest;
import com.booking.auth.dto.AuthDtos.LogoutRequest;
import com.booking.auth.dto.AuthDtos.RefreshRequest;
import com.booking.auth.dto.AuthDtos.RegisterRequest;
import com.booking.auth.dto.AuthDtos.TokenResponse;
import com.booking.auth.service.AuthService;
import com.booking.common.security.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Tag(name = "Auth", description = "Registration, login, refresh, logout")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @Operation(summary = "Register a new guest user")
    @PostMapping("/register/user")
    public Mono<TokenResponse> registerUser(@Valid @RequestBody RegisterRequest req) {
        return service.register(req, UserRole.USER);
    }

    @Operation(summary = "Register a hotel manager")
    @PostMapping("/register/manager")
    public Mono<TokenResponse> registerManager(@Valid @RequestBody RegisterRequest req) {
        return service.register(req, UserRole.MANAGER);
    }

    @Operation(summary = "Login and receive an access + refresh token pair")
    @PostMapping("/login")
    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    @Operation(summary = "Exchange a refresh token for a new access + refresh pair (rotation)")
    @PostMapping("/refresh")
    public Mono<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return service.refresh(req);
    }

    @Operation(summary = "Revoke the current access token (Authorization header) and refresh token (body)")
    @PostMapping("/logout")
    public Mono<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
                             @RequestBody(required = false) LogoutRequest body) {
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        String refreshToken = (body == null) ? null : body.refreshToken();
        return service.logout(accessToken, refreshToken);
    }
}
