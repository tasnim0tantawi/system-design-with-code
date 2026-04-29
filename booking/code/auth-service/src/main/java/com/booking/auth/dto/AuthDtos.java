package com.booking.auth.dto;

import com.booking.common.security.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 255) String name
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {}

    public record LogoutRequest(
            String refreshToken  // optional -- if omitted we still revoke the access token
    ) {}

    /**
     * Token pair returned on register, login, and refresh. The access token is
     * a short-lived JWT used on every API call. The refresh token is an opaque
     * random string used only against /api/auth/refresh to obtain a new pair.
     * Times are in seconds (OAuth 2 convention).
     */
    public record TokenResponse(
            String tokenType,
            String accessToken,
            long accessExpiresIn,
            String refreshToken,
            long refreshExpiresIn,
            long userId,
            UserRole role
    ) {}

    private AuthDtos() {}
}
