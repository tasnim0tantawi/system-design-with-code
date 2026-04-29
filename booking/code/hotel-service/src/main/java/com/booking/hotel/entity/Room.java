package com.booking.hotel.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("room")
public class Room {
    @Id
    private Integer id;

    @Column("hotel_id")
    private Integer hotelId;

    @Column("room_type")
    private String roomType;

    @Column("base_price")
    private BigDecimal basePrice;

    @Column("total_count")
    private Integer totalCount;
}
