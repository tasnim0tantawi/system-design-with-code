package com.booking.notification.service;

import com.booking.common.exception.ApiException;
import com.booking.notification.dto.NotificationDtos.*;
import com.booking.notification.entity.Notification;
import com.booking.notification.external.EmailSmsProvider;
import com.booking.notification.repository.NotificationRepository;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final EmailSmsProvider provider;
    private final ReactiveStringRedisTemplate redis;

    public NotificationService(NotificationRepository repo, EmailSmsProvider provider,
                               ReactiveStringRedisTemplate redis) {
        this.repo = repo;
        this.provider = provider;
        this.redis = redis;
    }

    public Mono<NotificationResponse> create(InternalRequest req) {
        Notification n = new Notification();
        n.setUserId(req.userId());
        n.setType(req.type());
        n.setChannel("email");
        n.setMessage(req.message());
        n.setStatus("queued");
        n.setCreatedAt(Instant.now());

        // POC: cache lookup of user prefs (we don't actually persist prefs).
        return cachedPrefs(req.userId())
                .then(repo.save(n))
                .flatMap(saved -> provider.sendEmail("user" + saved.getUserId() + "@example.com",
                                "[booking] " + saved.getType(), saved.getMessage())
                        .thenReturn(saved))
                .flatMap(saved -> {
                    saved.setStatus("sent");
                    return repo.save(saved);
                })
                .map(this::toResponse);
    }

    private Mono<String> cachedPrefs(int userId) {
        String key = "notif-prefs:" + userId;
        return redis.opsForValue().get(key)
                .switchIfEmpty(redis.opsForValue()
                        .set(key, "email=true,sms=false", Duration.ofSeconds(60))
                        .thenReturn("email=true,sms=false"));
    }

    public Mono<NotificationListResponse> listForUser(int userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId)
                .map(this::toResponse)
                .collectList()
                .map(items -> new NotificationListResponse(items, items.size()));
    }

    public Mono<NotificationResponse> markRead(int userId, int notifId) {
        return repo.findById(notifId)
                .switchIfEmpty(Mono.error(ApiException.notFound("notification " + notifId)))
                .flatMap(n -> {
                    if (n.getUserId() != userId) {
                        return Mono.error(ApiException.forbidden("not your notification"));
                    }
                    n.setReadAt(Instant.now());
                    n.setStatus("read");
                    return repo.save(n);
                })
                .map(this::toResponse);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getUserId(), n.getType(), n.getChannel(),
                n.getMessage(), n.getStatus(), n.getCreatedAt(), n.getReadAt());
    }
}
