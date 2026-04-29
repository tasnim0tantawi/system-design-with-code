package com.booking.auth.repository;

import com.booking.auth.entity.AuthUser;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface AuthUserRepository extends ReactiveCrudRepository<AuthUser, Integer> {
    Mono<AuthUser> findByEmail(String email);
    Mono<Boolean> existsByEmail(String email);
}
