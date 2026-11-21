package com.ticketflow.seat_service.service;

import com.ticketflow.seat_service.entity.Seat;
import com.ticketflow.seat_service.entity.SeatStatus;
import com.ticketflow.seat_service.exception.SeatNotFoundException;
import com.ticketflow.seat_service.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatReservationService {

    private final SeatRepository seatRepository;
    private final SeatAvailabilityService availabilityService;

    public SeatReservationService(
            SeatRepository seatRepository, SeatAvailabilityService availabilityService) {
        this.seatRepository = seatRepository;
        this.availabilityService = availabilityService;
    }

    @Transactional
    public boolean holdSeat(Long eventId, Long seatId) {
        int updated = seatRepository.holdIfAvailable(
                eventId, seatId, SeatStatus.AVAILABLE, SeatStatus.HELD);
        if (updated == 1) {
            Seat seat = seatRepository.findByIdAndEvent_Id(seatId, eventId)
                    .orElseThrow(() -> new SeatNotFoundException(eventId, seatId));
            availabilityService.publish(seat);
            return true;
        }
        if (!seatRepository.existsByIdAndEvent_Id(seatId, eventId)) {
            throw new SeatNotFoundException(eventId, seatId);
        }
        return false;
    }
}
