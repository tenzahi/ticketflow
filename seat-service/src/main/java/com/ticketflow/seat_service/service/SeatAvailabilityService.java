package com.ticketflow.seat_service.service;

import com.ticketflow.seat_service.entity.Seat;
import com.ticketflow.seat_service.entity.SeatStatus;
import com.ticketflow.seat_service.exception.EventNotFoundException;
import com.ticketflow.seat_service.grpc.AvailabilityUpdate;
import com.ticketflow.seat_service.repository.EventRepository;
import com.ticketflow.seat_service.repository.SeatRepository;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SeatAvailabilityService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final Map<Long, Set<StreamObserver<AvailabilityUpdate>>> subscribers = new HashMap<>();

    public SeatAvailabilityService(EventRepository eventRepository, SeatRepository seatRepository) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional(readOnly = true)
    public synchronized void watchAvailability(
            Long eventId, StreamObserver<AvailabilityUpdate> observer) {
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException(eventId);
        }

        List<AvailabilityUpdate> snapshot = seatRepository.findAllByEvent_Id(eventId).stream()
                .map(this::toUpdate)
                .toList();
        subscribers.computeIfAbsent(eventId, ignored -> new HashSet<>()).add(observer);

        if (observer instanceof ServerCallStreamObserver<?> serverObserver) {
            serverObserver.setOnCancelHandler(() -> unsubscribe(eventId, observer));
        }

        snapshot.forEach(observer::onNext);
    }

    public synchronized void publish(Seat seat) {
        Long eventId = seat.getEvent().getId();
        Set<StreamObserver<AvailabilityUpdate>> eventSubscribers = subscribers.get(eventId);
        if (eventSubscribers == null) {
            return;
        }

        AvailabilityUpdate update = toUpdate(seat);
        for (StreamObserver<AvailabilityUpdate> observer : new ArrayList<>(eventSubscribers)) {
            if (observer instanceof ServerCallStreamObserver<?> serverObserver
                    && serverObserver.isCancelled()) {
                eventSubscribers.remove(observer);
            } else {
                observer.onNext(update);
            }
        }
        if (eventSubscribers.isEmpty()) {
            subscribers.remove(eventId);
        }
    }

    private synchronized void unsubscribe(
            Long eventId, StreamObserver<AvailabilityUpdate> observer) {
        Set<StreamObserver<AvailabilityUpdate>> eventSubscribers = subscribers.get(eventId);
        if (eventSubscribers == null) {
            return;
        }
        eventSubscribers.remove(observer);
        if (eventSubscribers.isEmpty()) {
            subscribers.remove(eventId);
        }
    }

    private AvailabilityUpdate toUpdate(Seat seat) {
        return AvailabilityUpdate.newBuilder()
                .setEventId(seat.getEvent().getId())
                .setSeatId(seat.getId())
                .setSeatLabel(seat.getSeatLabel())
                .setStatus(toGrpcStatus(seat.getStatus()))
                .build();
    }

    private com.ticketflow.seat_service.grpc.SeatStatus toGrpcStatus(SeatStatus status) {
        return switch (status) {
            case AVAILABLE -> com.ticketflow.seat_service.grpc.SeatStatus.SEAT_STATUS_AVAILABLE;
            case HELD -> com.ticketflow.seat_service.grpc.SeatStatus.SEAT_STATUS_HELD;
        };
    }
}
