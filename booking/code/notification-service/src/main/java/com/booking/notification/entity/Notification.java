package com.booking.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notification")
public class Notification {
    @Id
    private Integer id;

    @Column("user_id")
    private Integer userId;

    private String type;
    private String channel;
    private String message;
    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Column("read_at")
    private Instant readAt;
}
