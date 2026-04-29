package com.booking.notification.external;

import reactor.core.publisher.Mono;

public interface EmailSmsProvider {
    Mono<Void> sendEmail(String to, String subject, String body);
    Mono<Void> sendSms(String to, String body);
}
