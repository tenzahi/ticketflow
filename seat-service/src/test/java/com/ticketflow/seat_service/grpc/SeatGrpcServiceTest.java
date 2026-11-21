package com.ticketflow.seat_service.grpc;

import com.ticketflow.seat_service.exception.EventNotFoundException;
import com.ticketflow.seat_service.exception.SeatNotFoundException;
import com.ticketflow.seat_service.service.SeatAvailabilityService;
import com.ticketflow.seat_service.service.SeatReservationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatGrpcServiceTest {

    @Mock
    private SeatReservationService seatReservationService;

    @Mock
    private SeatAvailabilityService seatAvailabilityService;

    private SeatGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new SeatGrpcService(seatReservationService, seatAvailabilityService);
    }

    @Test
    void returnsWhetherTheSeatWasHeld() {
        when(seatReservationService.holdSeat(10L, 20L)).thenReturn(false);
        TestObserver<HoldSeatResponse> observer = new TestObserver<>();
        HoldSeatRequest request = HoldSeatRequest.newBuilder()
                .setEventId(10L)
                .setSeatId(20L)
                .build();

        grpcService.holdSeat(request, observer);

        assertEquals(1, observer.values.size());
        assertFalse(observer.values.get(0).getHeld());
        assertTrue(observer.completed);
    }

    @Test
    void rejectsNonPositiveIds() {
        TestObserver<HoldSeatResponse> observer = new TestObserver<>();
        HoldSeatRequest request = HoldSeatRequest.newBuilder()
                .setEventId(0L)
                .setSeatId(20L)
                .build();

        grpcService.holdSeat(request, observer);

        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(observer.error).getCode());
        verify(seatReservationService, never()).holdSeat(0L, 20L);
    }

    @Test
    void reportsUnknownSeatAsNotFound() {
        when(seatReservationService.holdSeat(10L, 20L))
                .thenThrow(new SeatNotFoundException(10L, 20L));
        TestObserver<HoldSeatResponse> observer = new TestObserver<>();
        HoldSeatRequest request = HoldSeatRequest.newBuilder()
                .setEventId(10L)
                .setSeatId(20L)
                .build();

        grpcService.holdSeat(request, observer);

        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(observer.error).getCode());
        assertTrue(observer.values.isEmpty());
    }

    @Test
    void startsAvailabilityStreamForTheRequestedEvent() {
        TestObserver<AvailabilityUpdate> observer = new TestObserver<>();
        WatchAvailabilityRequest request = WatchAvailabilityRequest.newBuilder()
                .setEventId(10L)
                .build();

        grpcService.watchAvailability(request, observer);

        verify(seatAvailabilityService).watchAvailability(10L, observer);
        assertFalse(observer.completed);
    }

    @Test
    void reportsUnknownEventAsNotFound() {
        doThrow(new EventNotFoundException(10L)).when(seatAvailabilityService).watchAvailability(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any());
        TestObserver<AvailabilityUpdate> observer = new TestObserver<>();
        WatchAvailabilityRequest request = WatchAvailabilityRequest.newBuilder()
                .setEventId(10L)
                .build();

        grpcService.watchAvailability(request, observer);

        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(observer.error).getCode());
    }

    private static class TestObserver<T> implements StreamObserver<T> {
        private final List<T> values = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}
