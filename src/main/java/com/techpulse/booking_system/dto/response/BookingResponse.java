package com.techpulse.booking_system.dto.response;

import com.techpulse.booking_system.entity.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long slotId;
    private Long userId;
    private BookingStatus bookingStatus;
    private LocalDateTime createdAt;
}
