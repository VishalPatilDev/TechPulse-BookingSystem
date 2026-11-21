package com.techpulse.booking_system.dto.request;

import com.techpulse.booking_system.entity.Role;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {
    private String username;
    private String password;

}
