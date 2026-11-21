package com.ticketflow.seat_service.service;

import com.ticketflow.seat_service.entity.SeatStatus;
import com.ticketflow.seat_service.exception.SeatNotFoundException;
import com.ticketflow.seat_service.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatReservationService {

    private final SeatRepository seatRepository;

    public SeatReservationService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Transactional
    public boolean holdSeat(Long eventId, Long seatId) {
        int updated = seatRepository.holdIfAvailable(
                eventId, seatId, SeatStatus.AVAILABLE, SeatStatus.HELD);
        if (updated == 1) {
            return true;
        }
        if (!seatRepository.existsByIdAndEvent_Id(seatId, eventId)) {
            throw new SeatNotFoundException(eventId, seatId);
        }
        return false;
    }
}
