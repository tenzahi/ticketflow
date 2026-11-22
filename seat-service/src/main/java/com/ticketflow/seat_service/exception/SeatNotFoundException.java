package com.ticketflow.seat_service.exception;

public class SeatNotFoundException extends RuntimeException {

    public SeatNotFoundException(Long eventId, Long seatId) {
        super("Seat " + seatId + " does not exist for event " + eventId);
    }
}
