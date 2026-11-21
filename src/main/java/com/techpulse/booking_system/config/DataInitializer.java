package com.techpulse.booking_system.config;

import com.techpulse.booking_system.entity.Role;
import com.techpulse.booking_system.entity.User;
import com.techpulse.booking_system.repository.UserRepository;
import com.techpulse.booking_system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.techpulse.booking_system.entity.Role.ADMIN;
import static com.techpulse.booking_system.entity.Role.USER;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if(!userRepository.existsByUsername("admin")){
            User admin =User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created: admin / admin123");
        }
        if(!userRepository.existsByUsername("user1")){
            User user1 = User.builder()
                    .username("user1")
                    .password(passwordEncoder.encode("user123"))
                    .role(USER)
                    .build();
            userRepository.save(user1);
            log.info("Defauilt test user created: user1 / user123");
        }

    }
}
