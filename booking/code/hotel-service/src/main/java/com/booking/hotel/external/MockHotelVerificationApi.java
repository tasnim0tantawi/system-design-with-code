package com.booking.hotel.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "external.mocks.enabled", havingValue = "true", matchIfMissing = true)
public class MockHotelVerificationApi implements HotelVerificationApi {

    private static final Logger log = LoggerFactory.getLogger(MockHotelVerificationApi.class);

    @Override
    public Mono<Boolean> verify(String businessCredentials) {
        log.info("[MockHotelVerificationApi] verify({})", businessCredentials);
        return Mono.just(true).delayElement(Duration.ofMillis(100));
    }
}
