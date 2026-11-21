package com.techpulse.booking_system.service;

import com.techpulse.booking_system.dto.request.BookingRequest;
import com.techpulse.booking_system.dto.response.BookingResponse;
import com.techpulse.booking_system.entity.*;
import com.techpulse.booking_system.exception.ResourceNotFoundException;
import com.techpulse.booking_system.exception.SlotNotAvailableException;
import com.techpulse.booking_system.exception.UnauthorizedException;
import com.techpulse.booking_system.repository.BookingRepository;
import com.techpulse.booking_system.repository.SlotRepository;
import com.techpulse.booking_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Slot availableSlot;
    private BookingRequest bookingRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("john")
                .role(Role.USER)
                .build();

        availableSlot = Slot.builder()
                .id(10L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .slotStatus(SlotStatus.AVAILABLE)
                .build();

        bookingRequest = BookingRequest.builder()
                .slotId(10L)
                .build();
    }

    // ==================== BOOK SLOT ====================

    @Test
    void bookSlot_success() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(slotRepository.findByIdWithLock(10L)).thenReturn(Optional.of(availableSlot));

        Booking savedBooking = Booking.builder()
                .id(100L)
                .slot(availableSlot)
                .user(user)
                .bookingStatus(BookingStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.bookSlot(bookingRequest, "john");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(10L, response.getSlotId());
        assertEquals(1L, response.getUserId());
        assertEquals(BookingStatus.ACTIVE, response.getBookingStatus());

        // Verify slot status was changed
        assertEquals(SlotStatus.BOOKED, availableSlot.getSlotStatus());
        verify(slotRepository).save(availableSlot);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void bookSlot_userNotFound_throwsException() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.bookSlot(bookingRequest, "john"));

        verify(slotRepository, never()).findByIdWithLock(any());
    }

    @Test
    void bookSlot_slotNotFound_throwsException() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(slotRepository.findByIdWithLock(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.bookSlot(bookingRequest, "john"));
    }

    @Test
    void bookSlot_slotAlreadyBooked_throwsException() {
        availableSlot.setSlotStatus(SlotStatus.BOOKED);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(slotRepository.findByIdWithLock(10L)).thenReturn(Optional.of(availableSlot));

        SlotNotAvailableException exception = assertThrows(
                SlotNotAvailableException.class,
                () -> bookingService.bookSlot(bookingRequest, "john")
        );

        assertTrue(exception.getMessage().contains("already booked"));
        verify(bookingRepository, never()).save(any());
    }

    // ==================== CANCEL BOOKING (USER) ====================

    @Test
    void cancelBooking_success() {
        Booking booking = Booking.builder()
                .id(100L)
                .slot(availableSlot)
                .user(user)
                .bookingStatus(BookingStatus.ACTIVE)
                .build();

        when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.cancelBooking(100L, "john");

        assertEquals(BookingStatus.CANCELLED, response.getBookingStatus());
        assertEquals(SlotStatus.AVAILABLE, availableSlot.getSlotStatus());

        verify(bookingRepository).save(booking);
        verify(slotRepository).save(availableSlot);
    }

    @Test
    void cancelBooking_notOwner_throwsUnauthorized() {
        User anotherUser = User.builder().id(2L).username("alice").build();

        Booking booking = Booking.builder()
                .id(100L)
                .slot(availableSlot)
                .user(anotherUser)
                .bookingStatus(BookingStatus.ACTIVE)
                .build();

        when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(booking));

        assertThrows(UnauthorizedException.class,
                () -> bookingService.cancelBooking(100L, "john"));
    }

    @Test
    void cancelBooking_alreadyCancelled_throwsException() {
        Booking booking = Booking.builder()
                .id(100L)
                .slot(availableSlot)
                .user(user)
                .bookingStatus(BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(booking));

        assertThrows(SlotNotAvailableException.class,
                () -> bookingService.cancelBooking(100L, "john"));
    }

    // ==================== CANCEL BOOKING (ADMIN) ====================

    @Test
    void cancelBookingAdmin_success() {
        Booking booking = Booking.builder()
                .id(100L)
                .slot(availableSlot)
                .user(user)
                .bookingStatus(BookingStatus.ACTIVE)
                .build();

        when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.cancelBookingAdmin(100L);

        assertEquals(BookingStatus.CANCELLED, response.getBookingStatus());
        assertEquals(SlotStatus.AVAILABLE, availableSlot.getSlotStatus());
    }
}