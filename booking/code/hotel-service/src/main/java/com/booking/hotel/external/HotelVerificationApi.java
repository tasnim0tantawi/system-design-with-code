package com.booking.hotel.external;

import reactor.core.publisher.Mono;

public interface HotelVerificationApi {
    Mono<Boolean> verify(String businessCredentials);
}
