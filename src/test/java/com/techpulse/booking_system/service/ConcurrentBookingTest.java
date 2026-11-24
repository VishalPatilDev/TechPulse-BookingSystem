package com.techpulse.booking_system.service;

import com.techpulse.booking_system.dto.request.BookingRequest;
import com.techpulse.booking_system.dto.response.BookingResponse;
import com.techpulse.booking_system.entity.Slot;
import com.techpulse.booking_system.entity.SlotStatus;
import com.techpulse.booking_system.entity.User;
import com.techpulse.booking_system.entity.Role;
import com.techpulse.booking_system.exception.SlotNotAvailableException;
import com.techpulse.booking_system.repository.SlotRepository;
import com.techpulse.booking_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConcurrentBookingTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Slot testSlot;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        // Clean previous test data if needed (optional)
        // Create one available slot
        testSlot = Slot.builder()
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .slotStatus(SlotStatus.AVAILABLE)
                .build();
        testSlot = slotRepository.save(testSlot);

        // Create multiple users
        testUsers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String username = "concurrent_user_" + i;
            if (!userRepository.existsByUsername(username)) {
                User user = User.builder()
                        .username(username)
                        .password(passwordEncoder.encode("password"))
                        .role(Role.USER)
                        .build();
                testUsers.add(userRepository.save(user));
            } else {
                testUsers.add(userRepository.findByUsername(username).get());
            }
        }
    }

    @Test
    @DisplayName("Only ONE booking should succeed when 10 users try to book the same slot concurrently")
    void testConcurrentBooking_OnlyOneSucceeds() throws InterruptedException {

        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);          // All threads wait for this
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads); // Wait for all to finish

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            final User user = testUsers.get(i);
            final Long slotId = testSlot.getId();

            executor.submit(() -> {
                try {
                    // Wait until all threads are ready
                    startLatch.await();

                    BookingRequest request = BookingRequest.builder()
                            .slotId(slotId)
                            .build();

                    BookingResponse response = bookingService.bookSlot(request, user.getUsername());
                    successCount.incrementAndGet();
                    System.out.println("SUCCESS by user: " + user.getUsername() + " | BookingId: " + response.getId());

                } catch (SlotNotAvailableException e) {
                    failureCount.incrementAndGet();
                    exceptions.add(user.getUsername() + " → " + e.getMessage());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    exceptions.add(user.getUsername() + " → Unexpected: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads at almost the same time
        startLatch.countDown();

        // Wait maximum 15 seconds for all threads to finish
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // Assertions
        assertTrue(finished, "Test timed out – some threads did not finish");

        System.out.println("\n========== CONCURRENT BOOKING RESULT ==========");
        System.out.println("Successful bookings : " + successCount.get());
        System.out.println("Failed bookings     : " + failureCount.get());
        System.out.println("Exceptions:");
        exceptions.forEach(System.out::println);
        System.out.println("================================================\n");

        // Core assertions
        assertEquals(1, successCount.get(), "Exactly one booking must succeed");
        assertEquals(numberOfThreads - 1, failureCount.get(), "Remaining attempts must fail");

        // Verify final state in DB
        Slot updatedSlot = slotRepository.findById(testSlot.getId()).orElseThrow();
        assertEquals(SlotStatus.BOOKED, updatedSlot.getSlotStatus(), "Slot must be BOOKED");
    }

    @Test
    @DisplayName("Concurrent booking of different slots should all succeed")
    void testConcurrentBooking_DifferentSlots() throws InterruptedException {

        // Create 5 different available slots
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Slot slot = Slot.builder()
                    .startTime(LocalDateTime.now().plusDays(3).plusHours(i))
                    .endTime(LocalDateTime.now().plusDays(3).plusHours(i + 1))
                    .slotStatus(SlotStatus.AVAILABLE)
                    .build();
            slots.add(slotRepository.save(slot));
        }

        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final User user = testUsers.get(i);
            final Long slotId = slots.get(i).getId();

            executor.submit(() -> {
                try {
                    startLatch.await();
                    BookingRequest request = BookingRequest.builder().slotId(slotId).build();
                    bookingService.bookSlot(request, user.getUsername());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(5, successCount.get(), "All bookings on different slots should succeed");
    }
}