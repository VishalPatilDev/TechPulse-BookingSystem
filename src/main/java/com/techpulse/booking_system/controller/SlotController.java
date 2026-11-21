package com.techpulse.booking_system.controller;

import com.techpulse.booking_system.dto.request.SlotRequest;
import com.techpulse.booking_system.dto.response.ApiResponse;
import com.techpulse.booking_system.dto.response.SlotResponse;
import com.techpulse.booking_system.service.SlotService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/slots")
public class SlotController {
    @Autowired
    private SlotService slotService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSlot(@Valid @RequestBody SlotRequest slotRequest){
        SlotResponse slotResponse = slotService.createSlot(slotRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Slot Created successfully",slotResponse));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> fetchSlots(){
        List<SlotResponse> slots = slotService.getAllSlots();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Slots Fetched ",slots));
    }


}
