package com.ticketflow.seat_service.exception;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(Long eventId) {
        super("Event " + eventId + " does not exist");
    }
}
