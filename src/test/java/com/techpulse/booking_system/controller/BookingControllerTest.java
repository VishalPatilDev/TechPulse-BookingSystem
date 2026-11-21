package com.techpulse.booking_system.controller;

import com.techpulse.booking_system.dto.request.BookingRequest;
import com.techpulse.booking_system.dto.response.ApiResponse;
import com.techpulse.booking_system.dto.response.BookingResponse;
import com.techpulse.booking_system.entity.BookingStatus;
import com.techpulse.booking_system.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private UserDetails userDetails;
    private BookingRequest bookingRequest;
    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        userDetails = User.withUsername("john")
                .password("password")
                .roles("USER")
                .build();

        bookingRequest = BookingRequest.builder()
                .slotId(10L)
                .build();

        bookingResponse = BookingResponse.builder()
                .id(100L)
                .slotId(10L)
                .userId(1L)
                .bookingStatus(BookingStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void bookSlot_success() {
        when(bookingService.bookSlot(any(BookingRequest.class), eq("john")))
                .thenReturn(bookingResponse);

        ResponseEntity<?> response = bookingController.bookSlot(bookingRequest, userDetails);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Booking created successfully", body.getMessage());
        assertEquals(bookingResponse, body.getData());

        verify(bookingService).bookSlot(any(BookingRequest.class), eq("john"));
    }

    @Test
    void cancelBooking_success() {
        BookingResponse cancelled = BookingResponse.builder()
                .id(100L)
                .slotId(10L)
                .userId(1L)
                .bookingStatus(BookingStatus.CANCELLED)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingService.cancelBooking(100L, "john")).thenReturn(cancelled);

        ResponseEntity<?> response = bookingController.cancelBooking(100L, userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Booking Cancelled ", body.getMessage());

        verify(bookingService).cancelBooking(100L, "john");
    }

    @Test
    void cancelBookingAdmin_success() {
        BookingResponse cancelled = BookingResponse.builder()
                .id(100L)
                .slotId(10L)
                .userId(1L)
                .bookingStatus(BookingStatus.CANCELLED)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingService.cancelBookingAdmin(100L)).thenReturn(cancelled);

        ResponseEntity<?> response = bookingController.cancelBookingAdmin(100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Booking cancelled by admin", body.getMessage());

        verify(bookingService).cancelBookingAdmin(100L);
    }
}