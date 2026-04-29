package com.booking.hotel.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("room_availability")
public class RoomAvailability {
    @Id
    private Integer id;

    @Column("room_id")
    private Integer roomId;

    private LocalDate date;

    @Column("available_count")
    private Integer availableCount;
}
