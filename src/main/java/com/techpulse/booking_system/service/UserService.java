package com.techpulse.booking_system.service;

import com.techpulse.booking_system.dto.request.UserRequestDto;
import com.techpulse.booking_system.dto.response.AuthResponse;
import com.techpulse.booking_system.entity.Role;
import com.techpulse.booking_system.entity.User;
import com.techpulse.booking_system.exception.UserAlreadyExistsException;
import com.techpulse.booking_system.repository.UserRepository;
import com.techpulse.booking_system.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    public AuthResponse register(@Valid UserRequestDto userRequestDto) {
        if (userRepository.existsByUsername(userRequestDto.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + userRequestDto.getUsername());
        }
        User user  = User.builder()
                .role(Role.USER)
                .username(userRequestDto.getUsername())
                .password(passwordEncoder.encode(userRequestDto.getPassword()))
                .build();
        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(savedUser.getUsername());
        log.info("New user registered: {}", user.getUsername());

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .username(savedUser.getUsername())
                .userRole(savedUser.getRole())
                .build();
    }
}
