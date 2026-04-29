package com.booking.notification.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "external.mocks.enabled", havingValue = "true", matchIfMissing = true)
public class MockEmailSmsProvider implements EmailSmsProvider {

    private static final Logger log = LoggerFactory.getLogger(MockEmailSmsProvider.class);

    @Override
    public Mono<Void> sendEmail(String to, String subject, String body) {
        log.info("[MockEmailSmsProvider] EMAIL to={} subject={} body={}", to, subject, body);
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendSms(String to, String body) {
        log.info("[MockEmailSmsProvider] SMS to={} body={}", to, body);
        return Mono.empty();
    }
}
