package com.techpulse.booking_system.repository;

import com.techpulse.booking_system.entity.Booking;
import com.techpulse.booking_system.entity.Slot;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<Slot,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("Select s from Slot s where s.id = :id")
    Optional<Slot> findByIdWithLock(@Param("id") Long id);
}
