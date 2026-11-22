package com.ticketflow.seat_service.repository;

import com.ticketflow.seat_service.entity.Seat;
import com.ticketflow.seat_service.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findAllByEvent_Id(Long eventId);

    Optional<Seat> findByIdAndEvent_Id(Long seatId, Long eventId);

    boolean existsByIdAndEvent_Id(Long seatId, Long eventId);

    @Modifying
    @Query("""
            update Seat s
            set s.status = :heldStatus
            where s.id = :seatId
              and s.event.id = :eventId
              and s.status = :availableStatus
            """)
    int holdIfAvailable(
            @Param("eventId") Long eventId,
            @Param("seatId") Long seatId,
            @Param("availableStatus") SeatStatus availableStatus,
            @Param("heldStatus") SeatStatus heldStatus
    );
}
