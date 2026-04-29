package com.booking.notification.repository;

import com.booking.notification.entity.Notification;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface NotificationRepository extends ReactiveCrudRepository<Notification, Integer> {
    Flux<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
