package com.booking.booking.external;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface PaymentGateway {
    record ChargeResult(boolean success, String chargeId, String failureReason) {}

    Mono<ChargeResult> charge(int bookingId, BigDecimal amount);

    Mono<ChargeResult> refund(int bookingId, BigDecimal amount);
}
