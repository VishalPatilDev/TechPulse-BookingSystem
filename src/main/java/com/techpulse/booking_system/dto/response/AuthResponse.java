package com.techpulse.booking_system.dto.response;

import com.techpulse.booking_system.entity.Role;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String token;
    private String username;
    private Role userRole;
}
