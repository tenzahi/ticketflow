package com.ticketflow.seat_service.repository;

import com.ticketflow.seat_service.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findAllByEvent_Id(Long eventId);

    Optional<Seat> findByIdAndEvent_Id(Long seatId, Long eventId);
}
