package com.booking.gateway.config;

import com.booking.common.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

// 86400000 ms = 24 hours
    @Bean
    public JwtService jwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.ttl-millis:86400000}") long ttl) {
        return new JwtService(secret, ttl);
    }
}
