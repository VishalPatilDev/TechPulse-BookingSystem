package com.techpulse.booking_system.service;

import com.techpulse.booking_system.dto.request.SlotRequest;
import com.techpulse.booking_system.dto.response.SlotResponse;
import com.techpulse.booking_system.entity.Slot;
import com.techpulse.booking_system.entity.SlotStatus;
import com.techpulse.booking_system.repository.SlotRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SlotService {

    @Autowired
    private SlotRepository slotRepository;

    @Transactional
    public SlotResponse createSlot(@Valid SlotRequest slotRequest) {
        if (!slotRequest.getEndTime().isAfter(slotRequest.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        Slot slot = Slot.builder()
                .startTime(slotRequest.getStartTime())
                .endTime(slotRequest.getEndTime())
                .slotStatus(SlotStatus.AVAILABLE)
                .build();

        Slot saved = slotRepository.save(slot);
        log.info("Slot created with id: {}",saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public List<SlotResponse> getAllSlots(){
        return slotRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    //Helper
    private SlotResponse toResponse(Slot saved) {
        return SlotResponse.builder()
                .id(saved.getId())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .slotStatus(saved.getSlotStatus())
                .build();
    }
}
