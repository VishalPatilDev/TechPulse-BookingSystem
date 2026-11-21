package com.techpulse.booking_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    private Long slotId;
    //private Long userId;

    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();


}
