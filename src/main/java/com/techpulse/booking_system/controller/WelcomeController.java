package com.techpulse.booking_system.controller;

import com.techpulse.booking_system.dto.request.AuthRequest;
import com.techpulse.booking_system.dto.request.UserRequestDto;
import com.techpulse.booking_system.dto.response.ApiResponse;
import com.techpulse.booking_system.dto.response.AuthResponse;
import com.techpulse.booking_system.entity.User;
import com.techpulse.booking_system.repository.UserRepository;
import com.techpulse.booking_system.service.UserService;
import com.techpulse.booking_system.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController

@RequestMapping("/welcome")
@Slf4j
public class WelcomeController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;


    @GetMapping
    public ResponseEntity<?> welcomeGreet(){
        return ResponseEntity.status(HttpStatus.OK)
                .body("Healthy Welcome");
    }



    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody AuthRequest authRequest) {

        Authentication authentication =authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtUtil.generateToken(authRequest.getUsername());
        User user = userRepository.findByUsername(authRequest.getUsername()).orElseThrow();

        log.info("User logged in: {}", authRequest.getUsername());
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .userRole(user.getRole())
                .build();
        return ResponseEntity.ok(
                ApiResponse.success("Login Successful : ", response)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRequestDto userRequestDto){
        AuthResponse authResponse = userService.register(userRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User Registered Successfully ",authResponse));
    }
}
