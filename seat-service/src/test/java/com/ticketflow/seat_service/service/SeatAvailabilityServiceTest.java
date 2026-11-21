package com.ticketflow.seat_service.service;

import com.ticketflow.seat_service.entity.Event;
import com.ticketflow.seat_service.entity.Seat;
import com.ticketflow.seat_service.entity.SeatStatus;
import com.ticketflow.seat_service.grpc.AvailabilityUpdate;
import com.ticketflow.seat_service.repository.EventRepository;
import com.ticketflow.seat_service.repository.SeatRepository;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatAvailabilityServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ServerCallStreamObserver<AvailabilityUpdate> streamObserver;

    private SeatAvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new SeatAvailabilityService(eventRepository, seatRepository);
    }

    @Test
    void sendsInitialAvailabilityAndSubsequentUpdates() {
        Seat availableSeat = seat(SeatStatus.AVAILABLE);
        when(eventRepository.existsById(10L)).thenReturn(true);
        when(seatRepository.findAllByEvent_Id(10L)).thenReturn(List.of(availableSeat));
        TestObserver observer = new TestObserver();

        service.watchAvailability(10L, observer);
        service.publish(seat(SeatStatus.HELD));

        assertEquals(2, observer.values.size());
        assertEquals(10L, observer.values.get(0).getEventId());
        assertEquals(20L, observer.values.get(0).getSeatId());
        assertEquals("A1", observer.values.get(0).getSeatLabel());
        assertEquals(com.ticketflow.seat_service.grpc.SeatStatus.SEAT_STATUS_AVAILABLE,
                observer.values.get(0).getStatus());
        assertEquals(com.ticketflow.seat_service.grpc.SeatStatus.SEAT_STATUS_HELD,
                observer.values.get(1).getStatus());
    }

    @Test
    void removesCancelledStreamSubscribers() {
        when(eventRepository.existsById(10L)).thenReturn(true);
        when(seatRepository.findAllByEvent_Id(10L)).thenReturn(List.of());
        AtomicReference<Runnable> cancellationHandler = new AtomicReference<>();
        Event event = mock(Event.class);
        Seat seat = mock(Seat.class);
        when(event.getId()).thenReturn(10L);
        when(seat.getEvent()).thenReturn(event);
        doAnswer(invocation -> {
            cancellationHandler.set(invocation.getArgument(0));
            return null;
        }).when(streamObserver).setOnCancelHandler(any(Runnable.class));

        service.watchAvailability(10L, streamObserver);
        cancellationHandler.get().run();
        service.publish(seat);

        verify(streamObserver, never()).onNext(any(AvailabilityUpdate.class));
    }

    private Seat seat(SeatStatus status) {
        Event event = mock(Event.class);
        Seat seat = mock(Seat.class);
        when(event.getId()).thenReturn(10L);
        when(seat.getEvent()).thenReturn(event);
        when(seat.getId()).thenReturn(20L);
        when(seat.getSeatLabel()).thenReturn("A1");
        when(seat.getStatus()).thenReturn(status);
        return seat;
    }

    private static class TestObserver implements StreamObserver<AvailabilityUpdate> {
        private final List<AvailabilityUpdate> values = new ArrayList<>();

        @Override
        public void onNext(AvailabilityUpdate value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable error) {
            throw new AssertionError(error);
        }

        @Override
        public void onCompleted() {
        }
    }
}
