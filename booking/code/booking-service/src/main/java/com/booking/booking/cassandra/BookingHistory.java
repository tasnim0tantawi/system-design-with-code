package com.booking.booking.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Table("booking_history")
public class BookingHistory {

    @PrimaryKey
    private BookingHistoryKey key;

    @Column("hotel_id")
    private int hotelId;

    @Column("room_id")
    private int roomId;

    @Column("check_in")
    private LocalDate checkIn;

    @Column("check_out")
    private LocalDate checkOut;

    /**
     * Terminal state the booking ended up in. Spring Data Cassandra maps enums
     * to/from text columns automatically via {@link Enum#name()} and
     * {@link Enum#valueOf(Class, String)}, so no custom codec is needed.
     */
    @Column("status")
    private BookingHistoryStatus status;

    @Column("total_price")
    private BigDecimal totalPrice;

    public BookingHistory() {}

    public BookingHistoryKey getKey() { return key; }
    public void setKey(BookingHistoryKey key) { this.key = key; }
    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public BookingHistoryStatus getStatus() { return status; }
    public void setStatus(BookingHistoryStatus status) { this.status = status; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
}
