package com.booking.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class NotificationDtos {

    public record InternalRequest(
            @NotNull @Positive Integer userId,
            @NotBlank @Size(max = 64) String type,
            @NotBlank @Size(max = 4000) String message
    ) {}

    public record NotificationResponse(Integer id, Integer userId, String type, String channel,
                                        String message, String status, Instant createdAt, Instant readAt) {}

    public record NotificationListResponse(List<NotificationResponse> items, int total) {}

    private NotificationDtos() {}
}
