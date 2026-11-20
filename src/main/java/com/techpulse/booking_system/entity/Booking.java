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

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
