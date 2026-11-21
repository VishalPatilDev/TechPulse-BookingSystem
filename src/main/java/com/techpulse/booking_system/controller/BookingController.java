package com.techpulse.booking_system.controller;

import com.techpulse.booking_system.dto.request.BookingRequest;
import com.techpulse.booking_system.dto.response.ApiResponse;
import com.techpulse.booking_system.dto.response.BookingResponse;
import com.techpulse.booking_system.service.BookingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("bookings")
public class BookingController {
    @Autowired
    private BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> bookSlot(@Valid @RequestBody BookingRequest bookingRequest,
                                      @AuthenticationPrincipal UserDetails userDetails){
        BookingResponse bookingResponse =bookingService.bookSlot(bookingRequest,userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", bookingResponse));
    }

    @PostMapping("{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails){
        BookingResponse bookingResponse = bookingService.cancelBooking(id,userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Booking Cancelled ",bookingResponse));
    }
    @PostMapping("/admin/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelBookingAdmin(@PathVariable Long id){
        BookingResponse response = bookingService.cancelBookingAdmin(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Booking cancelled by admin", response));
    }
}
