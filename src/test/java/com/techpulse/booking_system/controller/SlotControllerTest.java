package com.techpulse.booking_system.controller;

import com.techpulse.booking_system.dto.request.SlotRequest;
import com.techpulse.booking_system.dto.response.ApiResponse;
import com.techpulse.booking_system.dto.response.SlotResponse;
import com.techpulse.booking_system.entity.SlotStatus;
import com.techpulse.booking_system.service.SlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotControllerTest {

    @Mock
    private SlotService slotService;

    @InjectMocks
    private SlotController slotController;

    private SlotRequest slotRequest;
    private SlotResponse slotResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 11, 0);

        slotRequest = SlotRequest.builder()
                .startTime(start)
                .endTime(end)
                .build();

        slotResponse = SlotResponse.builder()
                .id(1L)
                .startTime(start)
                .endTime(end)
                .slotStatus(SlotStatus.AVAILABLE)
                .build();
    }

    @Test
    void createSlot_success() {
        when(slotService.createSlot(any(SlotRequest.class))).thenReturn(slotResponse);

        ResponseEntity<?> response = slotController.createSlot(slotRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Slot Created successfully", body.getMessage());
        assertEquals(slotResponse, body.getData());

        verify(slotService).createSlot(any(SlotRequest.class));
    }

    @Test
    void fetchSlots_success() {
        when(slotService.getAllSlots()).thenReturn(List.of(slotResponse));

        ResponseEntity<?> response = slotController.fetchSlots();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Slots Fetched ", body.getMessage());
        assertEquals(1, ((List<?>) body.getData()).size());

        verify(slotService).getAllSlots();
    }
}