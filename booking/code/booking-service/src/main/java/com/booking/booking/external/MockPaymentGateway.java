package com.booking.booking.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Deterministic mock: charges that end in .99 always fail (so tests can
 * assert the failure path without randomness).
 */
@Component
@ConditionalOnProperty(name = "external.mocks.enabled", havingValue = "true", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    @Override
    public Mono<ChargeResult> charge(int bookingId, BigDecimal amount) {
        log.info("[MockPaymentGateway] charge bookingId={} amount={}", bookingId, amount);
        return Mono.just(result(amount));
    }

    @Override
    public Mono<ChargeResult> refund(int bookingId, BigDecimal amount) {
        log.info("[MockPaymentGateway] refund bookingId={} amount={}", bookingId, amount);
        return Mono.just(new ChargeResult(true, "refund-" + UUID.randomUUID(), null));
    }

    private ChargeResult result(BigDecimal amount) {
        String s = amount.toPlainString();
        if (s.endsWith(".99")) {
            return new ChargeResult(false, null, "card_declined");
        }
        return new ChargeResult(true, "ch-" + UUID.randomUUID(), null);
    }
}
