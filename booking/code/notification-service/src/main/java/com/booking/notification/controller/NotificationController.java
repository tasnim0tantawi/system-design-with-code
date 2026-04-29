package com.booking.notification.controller;

import com.booking.notification.dto.NotificationDtos.*;
import com.booking.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Tag(name = "Notifications", description = "User notification inbox")
@RestController
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @Operation(summary = "List notifications for a user")
    @GetMapping("/api/users/{userId}/notifications")
    public Mono<NotificationListResponse> list(@PathVariable int userId) {
        return service.listForUser(userId);
    }

    @Operation(summary = "Mark a notification as read")
    @PostMapping("/api/users/{userId}/notifications/{notifId}/read")
    public Mono<NotificationResponse> markRead(@PathVariable int userId, @PathVariable int notifId) {
        return service.markRead(userId, notifId);
    }

    @Hidden
    @PostMapping("/internal/notifications")
    public Mono<NotificationResponse> internalCreate(@Valid @RequestBody InternalRequest req) {
        return service.create(req);
    }
}
