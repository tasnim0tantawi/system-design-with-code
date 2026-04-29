package com.booking.auth.config;

import com.booking.common.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtService jwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-ttl-millis:900000}") long accessTtl) {
        // The default TTL on this bean is the access-token TTL. Refresh tokens
        // are opaque random strings, not JWTs, so they don't go through JwtService.
        return new JwtService(secret, accessTtl);
    }
}
