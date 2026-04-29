package com.booking.hotel.external;

import reactor.core.publisher.Mono;

public interface ObjectStorage {
    record PresignedUpload(String key, String publicUrl, String uploadUrl) {}

    Mono<PresignedUpload> presign(String contentType);
}
