package com.booking.review.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "external.mocks.enabled", havingValue = "true", matchIfMissing = true)
public class MockObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(MockObjectStorage.class);

    @Override
    public Mono<PresignedUpload> presign(String contentType) {
        String key = "review-images/" + UUID.randomUUID() + ".bin";
        PresignedUpload r = new PresignedUpload(
                key,
                "https://cdn.example.com/" + key,
                "https://upload.example.com/" + key + "?sig=fake"
        );
        log.info("[MockObjectStorage] presign({}) -> {}", contentType, r);
        return Mono.just(r);
    }
}
