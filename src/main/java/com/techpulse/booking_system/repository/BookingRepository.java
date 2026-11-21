package com.techpulse.booking_system.repository;

import com.techpulse.booking_system.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking,Long> {
    @Query("select b from Booking b join fetch b.slot join fetch b.user where b.id = :id")
    Optional<Booking>findByIdWithDetails(@Param("id")Long id);
}
