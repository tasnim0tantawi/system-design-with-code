package com.booking.review.entity;

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
@Table("review")
public class Review {
    @Id
    private Integer id;

    @Column("user_id")
    private Integer userId;

    @Column("hotel_id")
    private Integer hotelId;

    @Column("booking_id")
    private Integer bookingId;

    private Integer rating;
    private String comment;

    @Column("created_at")
    private Instant createdAt;
}
