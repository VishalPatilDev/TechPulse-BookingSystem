package com.techpulse.booking_system.controller;

import com.techpulse.booking_system.dto.request.AuthRequest;
import com.techpulse.booking_system.dto.request.UserRequestDto;
import com.techpulse.booking_system.dto.response.ApiResponse;
import com.techpulse.booking_system.dto.response.AuthResponse;
import com.techpulse.booking_system.entity.Role;
import com.techpulse.booking_system.entity.User;
import com.techpulse.booking_system.repository.UserRepository;
import com.techpulse.booking_system.service.UserService;
import com.techpulse.booking_system.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WelcomeControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WelcomeController welcomeController;

    @Test
    void welcomeGreet_success() {
        ResponseEntity<?> response = welcomeController.welcomeGreet();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Healthy Welcome", response.getBody());
    }

    @Test
    void login_success() {
        AuthRequest authRequest = AuthRequest.builder()
                .username("john")
                .password("password123")
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtUtil.generateToken("john")).thenReturn("jwt-token");

        User user = User.builder()
                .id(1L)
                .username("john")
                .role(Role.USER)
                .build();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = welcomeController.login(authRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Login Successful : ", body.getMessage());

        AuthResponse data = (AuthResponse) body.getData();
        assertEquals("jwt-token", data.getToken());
        assertEquals("john", data.getUsername());
        assertEquals(Role.USER, data.getUserRole());
    }

    @Test
    void register_success() {
        UserRequestDto requestDto = UserRequestDto.builder()
                .username("john")
                .password("password123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .id(1L)
                .token("jwt-token")
                .username("john")
                .userRole(Role.USER)
                .build();

        when(userService.register(any(UserRequestDto.class))).thenReturn(authResponse);

        ResponseEntity<?> response = welcomeController.register(requestDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("User Registered Successfully ", body.getMessage());
        assertEquals(authResponse, body.getData());
    }
}