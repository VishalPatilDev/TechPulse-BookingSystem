package com.techpulse.booking_system.service;

import com.techpulse.booking_system.dto.request.SlotRequest;
import com.techpulse.booking_system.dto.response.SlotResponse;
import com.techpulse.booking_system.entity.Slot;
import com.techpulse.booking_system.entity.SlotStatus;
import com.techpulse.booking_system.repository.SlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    @Mock
    private SlotRepository slotRepository;

    @InjectMocks
    private SlotService slotService;
    

    private SlotRequest validRequest;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2026, 11, 25, 10, 0);
        end = LocalDateTime.of(2026, 11, 25, 11, 0);

        validRequest = SlotRequest.builder()
                .startTime(start)
                .endTime(end)
                .build();
    }

    @Test
    void createSlot_success() {
        Slot savedSlot = Slot.builder()
                .id(1L)
                .startTime(start)
                .endTime(end)
                .slotStatus(SlotStatus.AVAILABLE)
                .build();

        when(slotRepository.save(any(Slot.class))).thenReturn(savedSlot);

        SlotResponse response = slotService.createSlot(validRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(start, response.getStartTime());
        assertEquals(end, response.getEndTime());
        assertEquals(SlotStatus.AVAILABLE, response.getSlotStatus());

        verify(slotRepository).save(any(Slot.class));
    }

    @Test
    void createSlot_endTimeBeforeStartTime_throwsException() {
        SlotRequest invalidRequest = SlotRequest.builder()
                .startTime(end)
                .endTime(start)   // end before start
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> slotService.createSlot(invalidRequest)
        );

        assertEquals("End time must be after start time", exception.getMessage());
        verify(slotRepository, never()).save(any());
    }

    @Test
    void getAllSlots_success() {
        Slot slot1 = Slot.builder().id(1L).startTime(start).endTime(end)
                .slotStatus(SlotStatus.AVAILABLE).build();
        Slot slot2 = Slot.builder().id(2L).startTime(start.plusHours(1)).endTime(end.plusHours(1))
                .slotStatus(SlotStatus.BOOKED).build();

        when(slotRepository.findAll()).thenReturn(List.of(slot1, slot2));

        List<SlotResponse> responses = slotService.getAllSlots();

        assertEquals(2, responses.size());
        assertEquals(SlotStatus.AVAILABLE, responses.get(0).getSlotStatus());
        assertEquals(SlotStatus.BOOKED, responses.get(1).getSlotStatus());
    }
}