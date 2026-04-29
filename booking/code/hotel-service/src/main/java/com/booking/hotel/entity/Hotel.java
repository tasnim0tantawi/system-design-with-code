package com.booking.hotel.entity;

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
@Table("hotel")
public class Hotel {
    @Id
    private Integer id;

    @Column("manager_id")
    private Integer managerId;

    private String name;
    private String description;
    private String location;
    private Integer stars;
    private String type;
    /** pending_verification | verified | suspended */
    private String status;

    @Column("created_at")
    private Instant createdAt;
}
