package com.techpulse.booking_system.service;

import com.techpulse.booking_system.dto.request.UserRequestDto;
import com.techpulse.booking_system.dto.response.AuthResponse;
import com.techpulse.booking_system.entity.Role;
import com.techpulse.booking_system.entity.User;
import com.techpulse.booking_system.exception.UserAlreadyExistsException;
import com.techpulse.booking_system.repository.UserRepository;
import com.techpulse.booking_system.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private UserRequestDto requestDto;

    @BeforeEach
    void setUp() {
        requestDto = UserRequestDto.builder()
                .username("john")
                .password("password123")
                .build();
    }

    @Test
    void register_success() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(jwtUtil.generateToken("john")).thenReturn("jwt-token");

        // Simulate JPA setting the ID when save() is called
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            userToSave.setId(1L);               // important
            return userToSave;
        });

        AuthResponse response = userService.register(requestDto);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("john", response.getUsername());
        assertEquals("jwt-token", response.getToken());
        assertEquals(Role.USER, response.getUserRole());

        verify(userRepository).existsByUsername("john");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
        verify(jwtUtil).generateToken("john");
    }

    @Test
    void register_usernameAlreadyExists_throwsException() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.register(requestDto)
        );

        assertEquals("Username already taken: john", exception.getMessage());
        verify(userRepository, never()).save(any());
    }
}