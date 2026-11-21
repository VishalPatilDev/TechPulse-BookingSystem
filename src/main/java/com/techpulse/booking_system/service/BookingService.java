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
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class BookingService {
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private UserRepository userRepository;

    //Helper Method
    private BookingResponse toResponse(Booking booking){
        return BookingResponse.builder()
                .id(booking.getId())
                .slotId(booking.getSlot().getId())
                .userId(booking.getUser().getId())
                .createdAt(booking.getCreatedAt())
                .bookingStatus(booking.getBookingStatus())
                .build();
    }

    @Transactional
    public BookingResponse bookSlot(@Valid BookingRequest bookingRequest, String username) {
        User user  = userRepository.findByUsername(username)
                .orElseThrow(()->new ResourceNotFoundException("User Not Found : "+username));

        Slot slot = slotRepository.findByIdWithLock(bookingRequest.getSlotId())
                .orElseThrow(()->
                        new ResourceNotFoundException("Slot not found with id : "+bookingRequest.getSlotId()));

        try {
            Thread.sleep(3000);   // 3 seconds – perfect for demo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if(slot.getSlotStatus() == SlotStatus.BOOKED){
            throw new SlotNotAvailableException("Slot "+bookingRequest.getSlotId()+" is already booked !");
        }
        slot.setSlotStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);
        Booking booking = Booking.builder()
                .slot(slot)
                .user(user)
                .bookingStatus(BookingStatus.ACTIVE)
                .build();
        Booking saved =bookingRepository.save(booking);
        log.info("Booking Created: id={}, slot={], user={}",saved.getId(),slot.getId(), username);
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse cancelBooking(Long id, String username) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(()->
                        new ResourceNotFoundException("Booking Not found with id : "+id));
        if(!booking.getUser().getUsername().equals(username)){
            throw new UnauthorizedException("you can only cancel your own bookings");
        }
        return performCancellation(booking);
    }
    @Transactional
    public BookingResponse cancelBookingAdmin(Long id){
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + id));
        return performCancellation(booking);
    }


    private BookingResponse performCancellation(Booking booking){
        if(booking.getBookingStatus() == BookingStatus.CANCELLED){
            throw new SlotNotAvailableException("Booking is already Cancelled");
        }
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        Slot slot = booking.getSlot();
        slot.setSlotStatus(SlotStatus.AVAILABLE);
        slotRepository.save(slot);
        log.info("Booking cancelled: id={}, slot freed: {}",booking.getId(),slot.getId());
        return toResponse(booking);
    }
}
