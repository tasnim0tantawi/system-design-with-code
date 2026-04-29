package com.booking.booking.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("booking")
public class Booking {
    @Id
    private Integer id;

    @Column("user_id")
    private Integer userId;

    @Column("hotel_id")
    private Integer hotelId;

    @Column("room_id")
    private Integer roomId;

    @Column("check_in")
    private LocalDate checkIn;

    @Column("check_out")
    private LocalDate checkOut;

    /**
     * Mapped to/from the varchar {@code status} column via the R2DBC
     * converters registered in {@code R2dbcConfig}.
     */
    private BookingStatus status;

    @Column("total_price")
    private BigDecimal totalPrice;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
